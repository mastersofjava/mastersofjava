package nl.moj.server.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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

    public Test(Team team, String assignment, String testname, Integer success, Integer failure) {
        super();
        this.team = team;
        this.assignment = assignment;
        this.testname = testname;
        this.success = success;
        this.failure = failure;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name="team_id")
	private Team team;

    @Column(name = "score")
    private String assignment;

    @Column(name = "score")
    private String testname;

    @Column(name = "score")
    private Integer success;

    @Column(name = "score")
    private Integer failure;
}
