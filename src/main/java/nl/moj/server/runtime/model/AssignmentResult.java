package nl.moj.server.runtime.model;

import lombok.*;

import javax.persistence.*;
import java.util.UUID;

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
}
