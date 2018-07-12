package nl.moj.server.git;

import java.time.Duration;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Ejnar Kaekebeke
 */
@Data
@Entity
@Table(name = "assignments")
@NoArgsConstructor(force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitAssignment {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    @JsonProperty("name")
    private String name;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "duration", nullable = false)
    @JsonProperty("duration")
    private Duration duration;

    @Column(name = "git_repository_path", unique = true, nullable = false)
    private String gitRepositoryPath;

    @Embedded
    @JsonProperty("scoring-rules")
    private ScoringRules scoringRules;

    @ManyToOne
    @JoinColumn(name="competition_id", nullable = false)
    private Competition competition;

    @ManyToOne
    @JoinColumn(name="git_repository_id", nullable = false)
    private GitRepository gitRepository;

}
