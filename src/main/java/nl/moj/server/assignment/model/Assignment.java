package nl.moj.server.assignment.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * @author Ejnar Kaekebeke
 * @author Bas Passon
 */
@Data
@Entity
@Table(name = "assignments")
@NoArgsConstructor(force = true)
@SequenceGenerator(name="id_seq", sequenceName = "assignments_seq")
public class Assignment {

	@Id
	@GeneratedValue(generator = "id_seq", strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;
	
	@Column(name = "name", unique = true, nullable = false)
	private String name;
	
	@Column(name = "assignment_descriptor", unique = true, nullable = false)
	private String assignmentDescriptor;
	
}
