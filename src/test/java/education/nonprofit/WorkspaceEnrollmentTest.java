package education.nonprofit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WorkspaceEnrollmentTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void matchingEmployeeJoinsOnlyAfterTxtVerification() throws Exception {
        InfraiGateway gateway = mock(InfraiGateway.class);
        when(gateway.addDomain("school.org")).thenReturn(json.readTree("{\"zone_id\":\"zone-7\"}"));
        when(gateway.createUser("teacher@school.org", "Teacher", "school.org"))
                .thenReturn(json.readTree("{\"id\":\"user-8\"}"));
        WorkspaceEnrollment.Enrollment result = new WorkspaceEnrollment(gateway).enroll(
                new WorkspaceEnrollment.JoinRequest("school.org", "teacher@school.org", "Teacher", "_ownership", "course-team-proof"));
        var order = inOrder(gateway);
        order.verify(gateway).addDomain("school.org");
        order.verify(gateway).putProof("zone-7", "_ownership", "course-team-proof");
        order.verify(gateway).verifyDomain("school.org");
        order.verify(gateway).createUser("teacher@school.org", "Teacher", "school.org");
        assertEquals("user-8", result.user().path("id").asText());
        assertEquals("campaign report for school.org", result.programPlan().campaignReport());
    }

    @Test
    void unrelatedEmailNeverStartsDomainProof() {
        InfraiGateway gateway = mock(InfraiGateway.class);
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceEnrollment(gateway).enroll(
                new WorkspaceEnrollment.JoinRequest("school.org", "teacher@other.org", "Teacher", "_ownership", "proof")));
        verifyNoInteractions(gateway);
    }
}
