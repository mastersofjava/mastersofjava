package nl.moj.server.git;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "git_repositories")
@Data
public class GitRepository {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "uri", nullable = false)
    private String uri;

    @Column(name = "clone_key", nullable = false)
    private String cloneKey;

    @Column(name = "branch", nullable = false)
    private String branch;
}
