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
package nl.moj.server;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.runtime.repository.AssignmentStatusRepository;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbUtil {


    private final CompetitionRepository competitionRepository;
    private final AssignmentRepository assignmentRepository;
    private final TeamRepository teamRepository;
    private final CompetitionSessionRepository competitionSessionRepository;
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final UserRepository userRepository;

    @Transactional
    public void cleanup() {
        teamAssignmentStatusRepository.deleteAll();
        competitionSessionRepository.deleteAll();
        competitionRepository.deleteAll();
        assignmentRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
    }

}
