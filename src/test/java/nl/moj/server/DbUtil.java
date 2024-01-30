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

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nl.moj.server.assignment.repository.AssignmentRepository;
import nl.moj.server.competition.repository.CompetitionRepository;
import nl.moj.server.competition.repository.CompetitionSessionRepository;
import nl.moj.server.runtime.repository.AssignmentResultRepository;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.repository.TeamRepository;
import nl.moj.server.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DbUtil {

    private static final Logger log = LoggerFactory.getLogger(DbUtil.class);

    private final CompetitionRepository competitionRepository;
    private final AssignmentRepository assignmentRepository;
    private final TeamRepository teamRepository;
    private final CompetitionSessionRepository competitionSessionRepository;
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final UserRepository userRepository;
    private final AssignmentResultRepository assignmentResultRepository;

    @Transactional
    public void cleanup() {
        log.info("Cleaning DB");
        assignmentResultRepository.deleteAll();
        teamAssignmentStatusRepository.deleteAll();
        competitionSessionRepository.deleteAll();
        competitionRepository.deleteAll();
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();
        log.info("Cleaned DB");
    }

}
