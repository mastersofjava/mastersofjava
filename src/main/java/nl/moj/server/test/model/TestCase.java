package nl.moj.server.test.model;

import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "test_cases")
@SequenceGenerator(name = "id_seq", sequenceName = "test_cases_seq")

@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = {"uuid"})
@ToString(exclude = {"testAttempt"})
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttempt testAttempt;

    @Column(name = "date_time_start", nullable = false)
    private Instant dateTimeStart;

    @Column(name = "date_time_end", nullable = false)
    private Instant dateTimeEnd;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name="timeout", nullable = false)
    private boolean timeout;

    @Column(name="test_output", columnDefinition = "TEXT")
    private String testOutput;

}
