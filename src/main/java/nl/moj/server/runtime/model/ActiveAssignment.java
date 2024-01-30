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
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.assignment.descriptor.AssignmentDescriptor;
import nl.moj.common.assignment.descriptor.ExecutionModel;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;

@Getter
@Builder
@Slf4j
/**
 * DTO: Holds the state of the current assignment (the assignment itself, the session it is part of and how long it has been
 * running.)
 */
public class ActiveAssignment {

    private CompetitionSession competitionSession;
    private Assignment assignment;
    private AssignmentDescriptor assignmentDescriptor;

    private Duration timeElapsed;
    private Long secondsRemaining;
    private Duration timeRemaining;
    private List<AssignmentFile> assignmentFiles; // this is a reference to AssignmentExecutionModel.originalAssignmentFiles, which is a reference to assignmentService.getAssignmentFiles(model.assignment)
    private boolean running;

    @Override
    public String toString() {
        return "[" + assignment.getName() + ", " + competitionSession.getId() + "]";
    }

    public CompetitionSession.SessionType getSessionType() {
        if (competitionSession != null && competitionSession.getSessionType() != null) {
            return competitionSession.getSessionType();
        }
        return null;
    }

    public List<String> getTestNames() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType() == AssignmentFileType.TEST ||
                        f.getFileType() == AssignmentFileType.INVISIBLE_TEST)
                .map(AssignmentFile::getName).collect(Collectors.toList());
    }

    public List<UUID> getTestUuids() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType() == AssignmentFileType.TEST ||
                        f.getFileType() == AssignmentFileType.INVISIBLE_TEST)
                .map(AssignmentFile::getUuid).collect(Collectors.toList());
    }

    public List<AssignmentFile> getTestFiles() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType() == AssignmentFileType.TEST ||
                        f.getFileType() == AssignmentFileType.INVISIBLE_TEST)
                .collect(Collectors.toList());
    }

    public List<AssignmentFile> getSubmitTestFiles() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType() == AssignmentFileType.HIDDEN_TEST ||
                        f.getFileType() == AssignmentFileType.INVISIBLE_TEST ||
                        f.getFileType() == AssignmentFileType.TEST)
                .collect(Collectors.toList());
    }

    public List<AssignmentFile> getVisibleFiles() {
        return assignmentFiles
                .stream().filter(f -> f.getFileType().isVisible())
                .collect(Collectors.toList());
    }

    public List<AssignmentFile> getFiles() {
        return assignmentFiles;
    }

    public ExecutionModel getExecutionModel() {
        if (assignmentDescriptor == null || assignmentDescriptor.getExecutionModel() == null) {
            return ExecutionModel.PARALLEL;
        }
        return assignmentDescriptor.getExecutionModel();
    }

}
