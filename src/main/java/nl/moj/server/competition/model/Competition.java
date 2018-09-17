package nl.moj.server.competition.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.*;

@Data
@Entity
@Table(name = "competitions")
@NoArgsConstructor(force = true)
@SequenceGenerator(name="competitions_seq", sequenceName = "competitions_seq")
public class Competition {

	@Id
	@GeneratedValue(generator = "competitions_seq", strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name="uuid", unique=true, nullable=false)
	private UUID uuid;

	@Column(name="name", nullable = false)
	private String name;

	@OneToMany(mappedBy = "competition", cascade = CascadeType.ALL)
	private List<OrderedAssignment> assignments;
	
	public List<OrderedAssignment> getAssignmentsInOrder() {
		List<OrderedAssignment> copy = new ArrayList<>(assignments);
		copy.sort(Comparator.comparingInt(OrderedAssignment::getOrder));
		return copy;
	}
}
