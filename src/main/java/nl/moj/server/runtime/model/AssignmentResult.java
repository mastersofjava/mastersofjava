package nl.moj.server.runtime.model;

import lombok.*;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "assignment_results")
@SequenceGenerator(name="assignment_result_id_seq", sequenceName = "assignment_results_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of={"uuid"})
public class AssignmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assignment_result_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name="uuid", nullable = false, updatable = false)
    private UUID uuid;

    @OneToOne
    @JoinColumn(name="assignment_status_id", nullable = false)
    private AssignmentStatus assignmentStatus;

    @Column(name="submit_time")
    private Integer submitTime;

    @Column(name="score")
    private Integer score;

}
