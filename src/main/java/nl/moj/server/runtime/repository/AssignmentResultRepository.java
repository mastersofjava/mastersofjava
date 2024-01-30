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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.runtime.model.AssignmentResult;
import nl.moj.server.teams.model.Team;

@Repository
public interface AssignmentResultRepository extends JpaRepository<AssignmentResult, Long> {

    @Query("SELECT a FROM AssignmentResult a WHERE a.assignmentStatus.team = :team " +
            "AND a.assignmentStatus.assignment = :assignment " +
            "AND a.assignmentStatus.competitionSession = :session")
    AssignmentResult findByTeamAndAssignmentAndCompetitionSession(Team team, Assignment assignment, CompetitionSession session);

    @Query("SELECT a FROM AssignmentResult a WHERE a.assignmentStatus.competitionSession = :session")
    List<AssignmentResult> findByCompetitionSession(@Param("session") CompetitionSession session);
}
