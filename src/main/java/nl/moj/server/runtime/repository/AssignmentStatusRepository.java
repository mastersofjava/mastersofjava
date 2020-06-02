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
package nl.moj.server.runtime.repository;

import java.util.List;

import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.teams.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentStatusRepository extends JpaRepository<AssignmentStatus, Long> {

    List<AssignmentStatus> findByAssignmentAndCompetitionSession(Assignment assignment, CompetitionSession competitionSession);

    AssignmentStatus findByAssignmentAndCompetitionSessionAndTeam(Assignment assignment, CompetitionSession competitionSession, Team team);

    List<AssignmentStatus> findByCompetitionSessionAndTeam(CompetitionSession competitionSession, Team team);

    @Query(value = "select NAME,ASSIGNMENT_ID, max(FINAL_SCORE),min(DATE_TIME_START) from ASSIGNMENT_RESULTS a_r ,ASSIGNMENT_STATUSES a_s,ASSIGNMENTS a where a_s.ID=a_r.ASSIGNMENT_STATUS_ID and a.ID=a_s.ASSIGNMENT_ID group by a.ID", nativeQuery = true)
    List<String[]> getHighscoreList();

}
