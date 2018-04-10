package nl.moj.server.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name="team_id")
	private Team team;

    @Column(name = "assignment")
    private String assignment;

    @Column(name = "test_name")
    private String testName;

    @Column(name = "success")
    private Integer success;

    @Column(name = "failure")
    private Integer failure;
}
