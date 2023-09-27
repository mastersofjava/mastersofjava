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
package nl.moj.server.test.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.test.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    TestAttempt findByUuid(UUID testAttemptUuid);

    List<TestAttempt> findByAssignmentStatus(TeamAssignmentStatus assignment);

    @Query(value="select count(ca) from TestAttempt ca " +
            "where ca.assignmentStatus = ?1 and ca.dateTimeRegister > ?2")
    long countNewerAttempts(TeamAssignmentStatus tas, Instant dateRegistered);
}
