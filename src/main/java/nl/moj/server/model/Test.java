package nl.moj.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.moj.server.teams.model.Team;

import javax.persistence.*;

@Entity
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@Table(name = "test")
public class Test {

    public Test(Team team, String assignment, String testName, Integer success, Integer failure) {
        super();
        this.team = team;
        this.assignment = assignment;
        this.testName = testName;
        this.success = success;
        this.failure = failure;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name="team_id", nullable = false)
	private Team team;

    @Column(name = "assignment", nullable = false)
    private String assignment;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "success", nullable = false)
    private Integer success;

    @Column(name = "failure", nullable = false)
    private Integer failure;
}
