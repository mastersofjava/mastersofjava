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

import javax.persistence.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.UUID;

import lombok.*;

@Entity
@Table(name = "assignment_results")
@SequenceGenerator(name = "assignment_result_id_seq", sequenceName = "assignment_results_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = {"uuid"})
@ToString(exclude = {"assignmentStatus"})
public class AssignmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assignment_result_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @OneToOne
    @JoinColumn(name = "assignment_status_id", nullable = false)
    private AssignmentStatus assignmentStatus;

    @Column(name = "initial_score", nullable = false)
    private Long initialScore;

    @Column(name = "penalty", nullable = false)
    private Long penalty;

    @Column(name = "bonus", nullable = false)
    private Long bonus;

    @Column(name = "final_score", nullable = false)
    private Long finalScore;


    public boolean isAssignmentEnded(Instant maxStartTime) {
        if (assignmentStatus.getDateTimeEnd()!=null) {
            return true;
        }
        return assignmentStatus.getDateTimeStart().isBefore(maxStartTime);
    }
}
