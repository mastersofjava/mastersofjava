package nl.moj.server.compiler.model;

import lombok.*;
import nl.moj.server.runtime.model.AssignmentStatus;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compile_attempts")
@SequenceGenerator(name="compile_attempts_id_seq", sequenceName = "compile_attempts_seq")

@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of={"uuid"})
public class CompileAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compile_attempts_id_seq")
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

    @Column(name="compiler_output", columnDefinition = "TEXT")
    private String compilerOutput;
}
