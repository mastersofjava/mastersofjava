package nl.moj.server.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "result")
@SequenceGenerator(name = "id_seq", sequenceName = "result_id_seq")
public class Result {

    public Result(Team team, String assignment, Integer score, Integer penalty, Integer credit) {
        super();
        this.team = team;
        this.assignment = assignment;
        this.score = score;
        this.penalty = penalty;
        this.credit = credit;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @Column(name = "id", nullable = false)
	private Long id;

    @ManyToOne
    @JoinColumn(name="team_id")
	private Team team;

    @ManyToOne
    @JoinColumn(name="ranking_id")
    private Ranking ranking;

    @Column(name = "assignment")
	private String assignment;

    @Column(name = "score")
	private Integer score;

    @Column(name = "penalty")
	private Integer penalty;

    @Column(name = "credit")
	private Integer credit;
}
