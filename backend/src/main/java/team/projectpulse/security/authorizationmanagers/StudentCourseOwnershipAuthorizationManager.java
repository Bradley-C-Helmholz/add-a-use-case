package team.projectpulse.security.authorizationmanagers;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import team.projectpulse.student.StudentSecurityService;

import java.util.function.Supplier;

/**
 * Guards acting on one student's account as the course admin of her course, admitting only that admin.
 *
 * <p>Reads {@code studentId} and delegates to {@link StudentSecurityService#isCurrentUserAdminOfStudentCourse}. A
 * route composes this with {@link StudentInstructorAuthorizationManager} through {@code AuthorizationManagers.anyOf}
 * when the course admin and the course section's instructor are both allowed.
 */
@Component
public class StudentCourseOwnershipAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final StudentSecurityService studentSecurityService;


    public StudentCourseOwnershipAuthorizationManager(StudentSecurityService studentSecurityService) {
        this.studentSecurityService = studentSecurityService;
    }

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
        Integer studentId = PathVariables.readId(context, "studentId");
        if (studentId == null) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(
                this.studentSecurityService.isCurrentUserAdminOfStudentCourse(studentId));
    }

}
