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
import java.time.Duration;
import java.time.Instant;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;

@Data
@Entity
@Table(name = "assignment_statuses")
@NoArgsConstructor(force = true)
@SequenceGenerator(name = "assignment_status_id_seq", sequenceName = "assignment_status_seq")
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = {"competitionSession", "assignment"})
public class AssignmentStatus {

    @Id
    @GeneratedValue(generator = "assignment_status_id_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "time_remaining")
    private Duration timeRemaining;

    @Column(name = "date_time_start")
    private Instant dateTimeStart;

    @Column(name = "date_time_end")
    private Instant dateTimeEnd;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne
    @JoinColumn(name = "competition_session_id", nullable = false)
    private CompetitionSession competitionSession;

}
