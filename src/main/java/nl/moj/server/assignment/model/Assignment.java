package nl.moj.server.assignment.model;

import javax.persistence.*;
import java.util.UUID;

import lombok.*;

/**
 * @author Ejnar Kaekebeke
 * @author Bas Passon
 */
@Entity
@Table(name = "assignments")
@SequenceGenerator(name = "assignment_id_seq", sequenceName = "assignments_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = {"uuid"})
public class Assignment {

    @Id
    @GeneratedValue(generator = "assignment_id_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "assignment_descriptor", unique = true, nullable = false)
    private String assignmentDescriptor;

}
