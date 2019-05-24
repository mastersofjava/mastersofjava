package nl.moj.server.runtime.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.submit.model.SubmitAttempt;
import nl.moj.server.teams.model.Team;
import nl.moj.server.test.model.TestAttempt;

@Entity
@Table(name = "assignment_statuses", uniqueConstraints = @UniqueConstraint(name = "competition_assignment_team_unique", columnNames = {"competition_session_id", "assignment_id", "team_id"}))
@SequenceGenerator(name = "assignment_status_id_seq", sequenceName = "assignment_statuses_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = {"uuid"})
public class AssignmentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assignment_status_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
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

    @Column(name = "date_time_start")
    private Instant dateTimeStart;

    @Column(name = "date_time_end")
    private Instant dateTimeEnd;

    @Column(name = "assignment_duration")
    private Duration assignmentDuration;

    @OneToMany(mappedBy = "assignmentStatus", cascade = CascadeType.REMOVE)
    private List<CompileAttempt> compileAttempts;

    @OneToMany(mappedBy = "assignmentStatus", cascade = CascadeType.REMOVE)
    private List<TestAttempt> testAttempts;

    @OneToMany(mappedBy = "assignmentStatus", cascade = CascadeType.REMOVE)
    private List<SubmitAttempt> submitAttempts;

    @OneToOne(mappedBy = "assignmentStatus", cascade = CascadeType.REMOVE)
    private AssignmentResult assignmentResult;

}
