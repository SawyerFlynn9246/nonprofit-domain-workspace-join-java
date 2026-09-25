package education.nonprofit;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class NonprofitWorkspaceService {
    private final WorkspaceEnrollment enrollment;

    public NonprofitWorkspaceService(WorkspaceEnrollment enrollment) { this.enrollment = enrollment; }

    public static void main(String[] args) { SpringApplication.run(NonprofitWorkspaceService.class, args); }

    @PostMapping("/workspace/join")
    public WorkspaceEnrollment.Enrollment join(@RequestBody WorkspaceEnrollment.JoinRequest request) {
        return enrollment.enroll(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(InfraiGateway.Rejected.class)
    public ResponseEntity<Map<String, String>> rejected(InfraiGateway.Rejected error) {
        HttpStatusCode status = error.status >= 400 && error.status < 500 ? HttpStatusCode.valueOf(error.status) : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(Map.of("error", error.code, "detail", error.getMessage()));
    }
}
