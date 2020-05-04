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
package nl.moj.server.runtime.model;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.assignment.descriptor.AssignmentDescriptor;
import nl.moj.server.assignment.descriptor.ExecutionModel;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;

@Getter
@Builder
@Slf4j
public class ActiveAssignment {

    private CompetitionSession competitionSession;
    private Assignment assignment;
    private AssignmentDescriptor assignmentDescriptor;

    private Duration timeElapsed;
    private Long timeRemaining;
    private List<AssignmentFile> assignmentFiles;
    private boolean running;

    public List<String> getTestNames() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType() == AssignmentFileType.TEST)
                .map(AssignmentFile::getName).collect(Collectors.toList());
    }

    public List<AssignmentFile> getTestFiles() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType() == AssignmentFileType.TEST)
                .collect(Collectors.toList());
    }

    public List<AssignmentFile> getSubmitTestFiles() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType() == AssignmentFileType.HIDDEN_TEST ||
                        f.getFileType() == AssignmentFileType.TEST)
                .collect(Collectors.toList());
    }

    public List<AssignmentFile> getVisibleFiles() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType().isVisible())
                .collect(Collectors.toList());
    }

    public ExecutionModel getExecutionModel() {
        if (assignmentDescriptor.getExecutionModel() == null) {
            return ExecutionModel.PARALLEL;
        }
        return assignmentDescriptor.getExecutionModel();
    }
}
