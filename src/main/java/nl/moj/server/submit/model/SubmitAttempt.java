package nl.moj.server.submit.model;

import lombok.*;
import nl.moj.server.runtime.model.AssignmentStatus;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.test.model.TestAttempt;

import javax.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submit_attempts")
@SequenceGenerator(name="submit_attempt_id_seq", sequenceName = "submit_attempts_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of={"uuid"})
public class SubmitAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "submit_attempt_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name="uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name="date_time_start", nullable = false)
    private Instant dateTimeStart;

    @Column(name="date_time_end", nullable = false)
    private Instant dateTimeEnd;

    @ManyToOne
    @JoinColumn(name="assignment_status_id", nullable=false)
    private AssignmentStatus assignmentStatus;

    @Column(name="success", nullable = false)
    private boolean success;

    @Column(name="assignment_time_elapsed")
    private Duration assignmentTimeElapsed;

    @OneToOne
    @JoinColumn(name="compile_attempt_id")
    private CompileAttempt compileAttempt;

    @OneToOne
    @JoinColumn(name="test_attempt_id")
    private TestAttempt testAttempt;

}
