package nl.moj.server.teams.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@Table(name = "teams")
@NoArgsConstructor(force = true)
@SequenceGenerator(name="teams_seq", sequenceName = "teams_seq")
@EqualsAndHashCode(of={"name"})
public class Team {

	@Id
	@GeneratedValue(generator = "teams_seq", strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "name", nullable = false, unique = true)
	private String name;

	@Column(name = "password")
	private String password;

	@Column(name = "cpassword")
	private String cpassword; // WTF? Remove this asap

	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	private Role role;

	@Column(name = "country")
	private String country;

	@Column(name = "company")
	private String company;

	public Team(String name, Role role, String country, String company) {
		super();
		this.name = name;
		this.role = role;
		this.country = country;
		this.company = company;
	}
	
	public String getShortName() {
		return name.length() > 20 ? name.substring(0, 20) + "..." : name;
	}

}
