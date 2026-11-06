package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * UC-STU-deactivate-student 6b: the course admin of the student's course is admitted whether or not she is assigned
 * to the student's course section, and anyone else, or an id that names nothing, is a denial rather than an exception.
 */
@ExtendWith(MockitoExtension.class)
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;
    @Mock
    UserUtils userUtils;

    @InjectMocks
    StudentSecurityService studentSecurityService;

    Student john;


    @BeforeEach
    void setUp() {
        Instructor courseAdmin = new Instructor("b.wei@abc.edu", "Bingyang", "Wei", "b.wei@abc.edu", "123456", true, "admin instructor");
        courseAdmin.setId(1);

        Course course = new Course("COSC 40993 Senior Design", "Senior design project course");
        course.setCourseAdmin(courseAdmin);

        // The course admin is deliberately NOT added to the course section's instructors
        Section section = new Section();
        section.setSectionId(2);
        section.setCourse(course);

        this.john = new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", true, "student");
        this.john.setId(4);
        section.addStudent(this.john);
    }

    @Test
    @DisplayName("The course admin of the student's course is admitted without being assigned to her course section")
    void admitsTheCourseAdminOfTheStudentsCourse() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).isTrue();
    }

    @Test
    @DisplayName("The admin of another course is denied")
    void deniesTheAdminOfAnotherCourse() {
        given(this.userUtils.getUserId()).willReturn(3);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).isFalse();
    }

    @Test
    @DisplayName("The student herself is denied")
    void deniesTheStudentHerself() {
        given(this.userUtils.getUserId()).willReturn(4);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).isFalse();
    }

    @Test
    @DisplayName("An id that names no student is a denial, not an exception")
    void deniesWhenTheStudentDoesNotExist() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(999999)).willReturn(Optional.empty());

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(999999)).isFalse();
    }

}
