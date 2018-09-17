package nl.moj.server.assignment.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.UUID;

/**
 * @author Ejnar Kaekebeke
 * @author Bas Passon
 */
@Data
@Entity
@Table(name = "assignments")
@NoArgsConstructor(force = true)
@SequenceGenerator(name="id_seq", sequenceName = "assignments_seq")
@EqualsAndHashCode(of={"uuid"})
public class Assignment {

	@Id
	@GeneratedValue(generator = "id_seq", strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name="uuid", unique=true, nullable=false)
	private UUID uuid;
	
	@Column(name = "name", unique = true, nullable = false)
	private String name;
	
	@Column(name = "assignment_descriptor", unique = true, nullable = false)
	private String assignmentDescriptor;
	
}
