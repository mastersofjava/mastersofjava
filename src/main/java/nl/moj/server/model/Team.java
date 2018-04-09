package nl.moj.server.model;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
@Setter
@Table(name = "team")
@SequenceGenerator(name = "id_seq", sequenceName = "team_id_seq")
public class Team {

    public Team(String name, String role, String country, String company) {
        super();
        this.name = name;
        this.role = role;
        this.country = country;
        this.company = company;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name")
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

    @OneToOne(mappedBy = "team",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private Ranking ranking;

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
