package nl.moj.server.teams.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import nl.moj.server.model.Result;
import nl.moj.server.model.Test;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Table(name = "teams")
@NoArgsConstructor(force = true)
@SequenceGenerator(name="id_seq", sequenceName = "teams_seq")
public class Team {

	@Id
	@GeneratedValue(generator = "id_seq", strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;
	@Column(name = "name", nullable = false, unique = true)
	private String name;
	@Column(name = "password")
	private String password;
	@Column(name = "cpassword")
	private String cpassword;
	@Column(name = "role")
	private String role;
	@Column(name = "country")
	private String country;
	@Column(name = "company")
	private String company;

	public Team(String name, String role, String country, String company) {
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
