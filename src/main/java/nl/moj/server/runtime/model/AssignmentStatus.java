package nl.moj.server.runtime.model;

import lombok.*;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;

import javax.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assignment_statuses")
@SequenceGenerator(name="assignment_status_id_seq", sequenceName = "assignment_statuses_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of={"uuid"})
public class AssignmentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assignment_status_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name="uuid", nullable = false, updatable = false)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name="competition_session_id", nullable=false)
    private CompetitionSession competitionSession;

    @ManyToOne
    @JoinColumn(name="team_id", nullable=false)
    private Team team;

    @ManyToOne
    @JoinColumn(name="assignment_id",nullable=false)
    private Assignment assignment;

    @Column(name="date_time_start")
    private Instant dateTimeStart;

    @Column(name="date_time_end")
    private Instant dateTimeEnd;

    @Column(name="assignment_duration")
    private Duration assignmentDuration;

    @OneToMany(mappedBy = "assignmentStatus")
    private List<CompileAttempt> compileAttempts;

    @OneToMany(mappedBy = "assignmentStatus")
    private List<TestAttempt> testAttempts;

    @OneToMany(mappedBy = "assignmentStatus")
    private List<SubmitAttempt> submitAttempts;

    @OneToOne(mappedBy="assignmentStatus")
    private AssignmentResult assignmentResult;

}
