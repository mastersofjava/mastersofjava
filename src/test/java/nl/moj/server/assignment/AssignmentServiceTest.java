package nl.moj.server.assignment;

import java.util.List;

import nl.moj.server.DbUtil;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static nl.moj.server.TestUtil.classpathResourceToPath;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AssignmentServiceTest {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private DbUtil dbUtil;

    @Before
    public void before() {
        dbUtil.cleanup();
    }

    @Test
    public void shouldDiscoverAssignments() throws Exception {
        List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/assignments"));

        assignments.forEach(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getName()).isNotBlank();
            assertThat(a.getAssignmentDescriptor()).contains("/assignments/");
        });
    }

    @Test
    public void shouldUpdateAssignments() throws Exception {
        List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/assignments"));

        assertThat(assignments.size()).isEqualTo(2);
        assignments.forEach(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getName()).isNotBlank();
            assertThat(a.getAssignmentDescriptor()).contains("/assignments/");
        });

        List<Assignment> updatedAssignments = assignmentService.updateAssignments(classpathResourceToPath("/assignments-updated"));

        assertThat(updatedAssignments.size()).isEqualTo(2);
        updatedAssignments.forEach(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getName()).isNotBlank();
            assertThat(a.getAssignmentDescriptor()).contains("/assignments-updated/");

        });
    }

    @Test
    public void shouldGetAssignmentDescriptor() throws Exception {
        List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/assignments"));

        assertThat(assignments.size()).isEqualTo(2);
        assignments.forEach(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getName()).isNotBlank();
            assertThat(a.getAssignmentDescriptor()).contains("/assignments/");
        });


        assignments.forEach(a -> {
            AssignmentDescriptor d = assignmentService.getAssignmentDescriptor(a);
            assertThat(d).isNotNull();
            assertThat(d.getName()).isNotBlank();
            assertThat(d.getDuration()).isNotNull();
            assertThat(d.getScoringRules()).isNotNull();
            assertThat(d.getAssignmentFiles()).isNotNull();
        });
    }
}