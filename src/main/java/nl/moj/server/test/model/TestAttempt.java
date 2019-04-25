package nl.moj.server.test.model;

import lombok.*;
import nl.moj.server.runtime.model.AssignmentStatus;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_attempts")
@SequenceGenerator(name="id_seq", sequenceName = "test_attempts_seq")

@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of={"uuid"})
@ToString(exclude = {"assignmentStatus"})
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name="uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name="date_time_start", nullable = false)
    private Instant dateTimeStart;

    @Column(name="date_time_end")
    private Instant dateTimeEnd;

    @ManyToOne
    @JoinColumn(name="assignment_status_id", nullable=false)
    private AssignmentStatus assignmentStatus;

    @OneToMany(mappedBy = "testAttempt", cascade = CascadeType.REMOVE)
    private List<TestCase> testCases;
}
