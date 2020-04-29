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
package nl.moj.server.competition.repository;

import java.util.List;
import java.util.UUID;

import nl.moj.server.competition.model.Competition;
import nl.moj.server.competition.model.CompetitionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionSessionRepository extends JpaRepository<CompetitionSession, Long> {

    List<CompetitionSession> findByCompetition(Competition competition);

    CompetitionSession findByUuid(UUID session);
}

