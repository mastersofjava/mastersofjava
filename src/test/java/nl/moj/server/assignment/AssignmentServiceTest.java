/*
   Copyright 2020 First Eight BV (The Netherlands)
 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.assignment;

import java.io.File;
import java.util.List;

import nl.moj.server.DbUtil;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.assignment.service.AssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static nl.moj.server.TestUtil.classpathResourceToPath;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AssignmentServiceTest {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private DbUtil dbUtil;

    @BeforeEach
    public void before() {
        dbUtil.cleanup();
    }

    @Test
    public void shouldDiscoverAssignments() throws Exception {
        List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/assignments"), "assignments");

        assignments.forEach(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getName()).isNotBlank();
            assertThat(a.getAssignmentDescriptor()).contains(File.separator + "assignments" + File.separator);
        });
    }

    @Test
    public void shouldUpdateAssignments() throws Exception {
        List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/assignments"), "assignments");

        assertThat(assignments.size()).isEqualTo(2);
        assignments.forEach(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getName()).isNotBlank();
            assertThat(a.getAssignmentDescriptor()).contains(File.separator + "assignments" + File.separator);
        });

        List<Assignment> updatedAssignments = assignmentService.updateAssignments(classpathResourceToPath("/assignments-updated"), "assignments");

        assertThat(updatedAssignments.size()).isEqualTo(2);
        updatedAssignments.forEach(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getName()).isNotBlank();
            assertThat(a.getAssignmentDescriptor()).contains(File.separator+ "assignments-updated" + File.separator);

        });
    }

    @Test
    public void shouldGetAssignmentDescriptor() throws Exception {
        List<Assignment> assignments = assignmentService.updateAssignments(classpathResourceToPath("/assignments"), "assignments");

        assertThat(assignments.size()).isEqualTo(2);
        assignments.forEach(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getName()).isNotBlank();
            assertThat(a.getAssignmentDescriptor()).contains(File.separator + "assignments" + File.separator);
        });


        assignments.forEach(a -> {
            AssignmentDescriptor d = assignmentService.resolveAssignmentDescriptor(a);
            assertThat(d).isNotNull();
            assertThat(d.getName()).isNotBlank();
            assertThat(d.getDuration()).isNotNull();
            assertThat(d.getScoringRules()).isNotNull();
            assertThat(d.getAssignmentFiles()).isNotNull();
        });
    }
}