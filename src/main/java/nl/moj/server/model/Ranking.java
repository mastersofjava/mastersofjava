package nl.moj.server.model;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
@Table(name = "ranking")
@SequenceGenerator(name = "id_seq", sequenceName = "ranking_id_seq")
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "rank")
	private Integer rank;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
	private Team team;

    @OneToMany(mappedBy = "ranking")
	private List<Result> results;

    @Column(name = "total_score")
	private Integer totalScore;

	public String getResultJson() {
		StringBuffer b = new StringBuffer();
		b.append("{\"scores\":[");
		results.forEach( r -> {
			b.append("{\"name\":\"").append(r.getAssignment()).append("\",\"score\":").append(r.getScore()).append("},");
		});
		if( b.length() > 1) {
			b.deleteCharAt(b.length() - 1);
		}
		b.append("]}");
		return b.toString();
	}
}
