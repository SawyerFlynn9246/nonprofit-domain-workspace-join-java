package education.nonprofit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfraiGateway {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json;
    private final String baseUrl;
    private final String key;

    public InfraiGateway(ObjectMapper json, @Value("${infrai.base-url}") String baseUrl) {
        this.json = json;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    }

    public JsonNode addDomain(String domain) { return call("POST", "/v1/dns/domain/add", Map.of("domain", domain)); }
    public JsonNode putProof(String zoneId, String name, String proof) {
        return call("PUT", "/v1/dns/record/upsert", Map.of("zone_id", zoneId, "record_type", "TXT", "name", name, "content", proof));
    }
    public JsonNode verifyDomain(String domain) { return call("POST", "/v1/dns/domain/verify", Map.of("domain", domain)); }
    public JsonNode createUser(String email, String name, String domain) {
        return call("POST", "/v1/auth/user/create", Map.of("email", email, "name", name,
                "metadata", Map.of("workspace_domain", domain), "idempotency_key", "employee:" + email.toLowerCase()));
    }

    private JsonNode call(String method, String path, Map<String, Object> body) {
        try {
            String payload = json.writeValueAsString(body);
            for (int attempt = 0; attempt < 4; attempt++) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + key)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(20))
                        .method(method, HttpRequest.BodyPublishers.ofString(payload)).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 429 && attempt < 3) {
                    long seconds = response.headers().firstValue("Retry-After").map(InfraiGateway::retrySeconds)
                            .orElse(1L << attempt);
                    Thread.sleep(Math.max(1, seconds) * 1000);
                    continue;
                }
                JsonNode envelope = json.readTree(response.body());
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new Rejected(response.statusCode(), error.path("code").asText("REQUEST_REJECTED"), error.toString());
                }
                if (response.statusCode() >= 500) throw new IllegalStateException("Upstream HTTP " + response.statusCode());
                return envelope.path("data");
            }
            throw new IllegalStateException("Retry budget exhausted");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read API response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted", e);
        }
    }

    private static long retrySeconds(String value) {
        try { return Math.min(30, Long.parseLong(value)); }
        catch (NumberFormatException ignored) { return 1; }
    }

    public static class Rejected extends RuntimeException {
        public final int status;
        public final String code;
        public Rejected(int status, String code, String detail) {
            super(detail);
            this.status = status;
            this.code = code;
        }
    }
}
