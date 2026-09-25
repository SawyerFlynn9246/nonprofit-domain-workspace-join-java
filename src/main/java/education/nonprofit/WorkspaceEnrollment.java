package education.nonprofit;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceEnrollment {
    private final InfraiGateway gateway;

    public WorkspaceEnrollment(InfraiGateway gateway) { this.gateway = gateway; }

    public Enrollment enroll(JoinRequest request) {
        String domain = request.domain().toLowerCase(Locale.ROOT);
        String email = request.email().toLowerCase(Locale.ROOT);
        if (!email.endsWith("@" + domain)) throw new IllegalArgumentException("Employee email must match the workspace domain");
        if (request.proof().isBlank() || request.proofName().isBlank()) throw new IllegalArgumentException("TXT name and content are required");

        JsonNode zone = gateway.addDomain(domain);
        String zoneId = zone.path("zone_id").asText();
        if (zoneId.isBlank()) throw new IllegalStateException("Domain response has no zone_id");
        gateway.putProof(zoneId, request.proofName(), request.proof());
        gateway.verifyDomain(domain);
        JsonNode user = gateway.createUser(email, request.name(), domain);
        return new Enrollment(domain, email, user, new ProgramPlan(
                "donor receipt for " + domain, "volunteer reminder for " + domain,
                "campaign report for " + domain));
    }

    public record JoinRequest(String domain, String email, String name, String proofName, String proof) {}
    public record ProgramPlan(String donorReceipt, String volunteerReminder, String campaignReport) {}
    public record Enrollment(String workspaceDomain, String employeeEmail, JsonNode user, ProgramPlan programPlan) {}
}
