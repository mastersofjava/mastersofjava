package nl.moj.server.runtime.model;

import lombok.Data;
import nl.moj.server.assignment.model.Assignment;
import nl.moj.server.competition.model.CompetitionSession;
import nl.moj.server.teams.model.Team;

import javax.persistence.*;

@Entity
@Data
@Table(name = "result", uniqueConstraints = {@UniqueConstraint(columnNames = {"team_id","competition_session_id","assignment_id"})})
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
	private Long id;

    @ManyToOne
    @JoinColumn(name="team_id", nullable = false)
	private Team team;

    @ManyToOne
	@JoinColumn(name = "competition_session_id", nullable = false)
    private CompetitionSession competitionSession;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
	private Assignment assignment;

    @Column(name = "score", nullable = false)
	private Integer score;

    @Column(name = "penalty", nullable = false)
	private Integer penalty;

    @Column(name = "credit", nullable = false)
	private Integer credit;
    
}
