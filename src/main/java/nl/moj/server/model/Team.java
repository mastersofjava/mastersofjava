package nl.moj.server.model;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@Table(name = "team")
public class Team {

    public Team(String name, String role, String country, String company) {
        super();
        this.name = name;
        this.role = role;
        this.country = country;
        this.company = company;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @OneToMany(mappedBy = "team")
    private List<Result> results;

    @OneToMany(mappedBy = "team")
    private List<Test> test;

	public Integer getTotalScore(){
        int sum = 0;

	    if (getResults() != null) {
            for (Result r : getResults()) {
                sum += r.getScore();
            }
        }

		return sum;
	}

	public String getShortName() {
	    return name.length() > 20 ? name.substring(0, 20) + "..." : name;
    }
	
}
