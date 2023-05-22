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
import java.util.*;
import java.util.stream.Collectors;

import lombok.*;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;

@Entity
@Table(name = "team_assignment_statuses", uniqueConstraints = @UniqueConstraint(name = "team_assignment_statuses_cs_a_t_uk", columnNames = {"competition_session_id", "assignment_id", "team_id"}))
@SequenceGenerator(name = "team_assignment_status_id_seq", sequenceName = "team_assignment_statuses_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = {"uuid"})
/**
 * Stores all attempts for an assignment for a specific team.
 */		
public class TeamAssignmentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_assignment_status_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "competition_session_id", nullable = false)
    private CompetitionSession competitionSession;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "date_time_start", nullable = false)
    private Instant dateTimeStart;

    @Column(name = "date_time_completed")
    private Instant dateTimeCompleted;

    @Column(name = "date_time_end")
    private Instant dateTimeEnd;

    @Builder.Default
    @OneToMany(mappedBy = "assignmentStatus", cascade = CascadeType.REMOVE)
    private List<CompileAttempt> compileAttempts = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "assignmentStatus", cascade = CascadeType.REMOVE)
    private List<TestAttempt> testAttempts = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "assignmentStatus", cascade = CascadeType.REMOVE)
    private List<SubmitAttempt> submitAttempts = new ArrayList<>();

    @OneToOne(mappedBy="assignmentStatus", cascade = CascadeType.REMOVE)
    private AssignmentResult assignmentResult;

    public TestAttempt getMostRecentTestAttempt() {
        List<TestAttempt> attempts = getTestAttempts().stream().filter( ta -> ta.getDateTimeEnd() != null ).collect(Collectors.toList());
        if( attempts.isEmpty()) {
            return null;
        }
        attempts.sort(Comparator.comparing(TestAttempt::getDateTimeEnd).reversed());
        return attempts.get(0);
    }

    public SubmitAttempt getMostRecentSubmitAttempt() {
        List<SubmitAttempt> attempts = getSubmitAttempts().stream().filter( ta -> ta.getDateTimeEnd() != null ).collect(Collectors.toList());
        if( attempts.isEmpty()) {
            return null;
        }
        attempts.sort(Comparator.comparing(SubmitAttempt::getDateTimeEnd).reversed());
        return attempts.get(0);
    }

    public int getRemainingSubmitAttempts() {
        // do not include aborted attempts.
        return getAssignment().getAllowedSubmits() - countNotAbortedSubmitAttempts();
    }

    public int countNotAbortedSubmitAttempts() {
        return Long.valueOf(getSubmitAttempts().stream()
                .filter( sa -> sa.getAborted() == null || sa.getAborted().equals(Boolean.FALSE)).count()).intValue();
    }
}
