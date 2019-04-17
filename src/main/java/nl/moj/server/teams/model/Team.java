package nl.moj.server.teams.model;

import lombok.*;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "teams")
@SequenceGenerator(name="team_id_seq", sequenceName = "teams_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of={"uuid"})
public class Team {

	@Id
	@GeneratedValue(generator = "team_id_seq", strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name="uuid", nullable = false, unique = true)
	private UUID uuid;

	@Column(name = "name", nullable = false, unique = true)
	private String name;

	@Column(name = "password")
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private Role role;

	@Column(name = "country")
	private String country;

	@Column(name = "company")
	private String company;

	public String getShortName() {
		return name.length() > 20 ? name.substring(0, 20) + "..." : name;
	}

}

