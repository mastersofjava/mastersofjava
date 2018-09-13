package nl.moj.server.competition.model;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.UUID;

@Data
@Entity
@Table(name = "competition_sessions")
@NoArgsConstructor(force = true)
@SequenceGenerator(name="competition_sessions_seq", sequenceName = "competition_sessions_seq")
public class CompetitionSession {

	@Id
	@GeneratedValue(generator = "competition_sessions_seq", strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name="uuid", unique=true, nullable=false)
	private UUID uuid;

	@ManyToOne
	@JoinColumn(name="competition_id", nullable = false)
	private Competition competition;
}
