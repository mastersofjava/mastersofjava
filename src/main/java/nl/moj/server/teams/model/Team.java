package nl.moj.server.teams.model;

import lombok.*;

import javax.persistence.*;
import java.util.UUID;

@Builder
@AllArgsConstructor(access=AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@EqualsAndHashCode(of={"name"})
@Table(name = "teams")
@Entity
@SequenceGenerator(name="teams_seq", sequenceName = "teams_seq")
public class Team {

	@Id
	@GeneratedValue(generator = "teams_seq", strategy = GenerationType.SEQUENCE)
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

