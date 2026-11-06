package team.projectpulse.security.authorizationmanagers;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import team.projectpulse.student.StudentSecurityService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StudentCourseOwnershipAuthorizationManagerTest {

    @Mock
    StudentSecurityService studentSecurityService;

    @InjectMocks
    StudentCourseOwnershipAuthorizationManager studentCourseOwnershipAuthorizationManager;


    private RequestAuthorizationContext contextWith(Map<String, String> variables) {
        return new RequestAuthorizationContext(mock(HttpServletRequest.class), variables);
    }

    @Test
    @DisplayName("Asks about the studentId the matched route bound")
    void delegatesTheBoundStudentId() {
        given(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).willReturn(true);

        AuthorizationResult result = this.studentCourseOwnershipAuthorizationManager.authorize(() -> null, contextWith(Map.of("studentId", "4")));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    @DisplayName("Denies when the matched route bound no studentId, without asking the service")
    void deniesWhenTheVariableIsAbsent() {
        AuthorizationResult result = this.studentCourseOwnershipAuthorizationManager.authorize(() -> null, contextWith(Map.of()));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verifyNoInteractions(this.studentSecurityService);
    }

    @Test
    @DisplayName("Denies when the studentId is not a number")
    void deniesWhenTheVariableIsNotANumber() {
        AuthorizationResult result = this.studentCourseOwnershipAuthorizationManager.authorize(() -> null, contextWith(Map.of("studentId", "abc")));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verifyNoInteractions(this.studentSecurityService);
    }

}
