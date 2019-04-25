package nl.moj.server.competition.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nl.moj.server.assignment.model.Assignment;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name="ordered_assignments", uniqueConstraints = {
		@UniqueConstraint(name="order_competition_unique", columnNames = {"idx","competition_id"})
})
@Data
@NoArgsConstructor(force = true)
@SequenceGenerator(name="ordered_assignments_seq", sequenceName = "ordered_assignments_seq")
@EqualsAndHashCode(of={"uuid"})
@ToString(exclude = {"competition"})
public class OrderedAssignment {

	@Id
	@GeneratedValue(generator = "ordered_assignments_seq", strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name="uuid", nullable=false, unique=true)
	private UUID uuid;
	
	@Column(name="idx", nullable=false)
	private Integer order;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="assignment_id", nullable=false)
	private Assignment assignment;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="competition_id", nullable=false)
	private Competition competition;

}
