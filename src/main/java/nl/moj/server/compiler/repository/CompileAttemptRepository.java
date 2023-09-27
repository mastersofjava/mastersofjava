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
package nl.moj.server.compiler.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CompileAttemptRepository extends JpaRepository<CompileAttempt, Long> {
    CompileAttempt findByUuid(UUID compileAttemptUuid);

    List<CompileAttempt> findByAssignmentStatus(TeamAssignmentStatus assignment);

    @Query(value="select count(ca) from CompileAttempt ca " +
            "where ca.assignmentStatus = ?1 and ca.dateTimeRegister > ?2")
    long countNewerAttempts(TeamAssignmentStatus tas, Instant dateRegistered);

}
