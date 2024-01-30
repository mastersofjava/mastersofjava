package nl.moj.server.user.model;

import javax.persistence.*;

import lombok.*;
import nl.moj.server.teams.model.Team;

@Entity
@Table(name = "users")
@SequenceGenerator(name = "users_id_seq", sequenceName = "users_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = { "name" })
public class User {

    @Id
    @GeneratedValue(generator = "users_id_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "given_name", nullable = false)
    private String givenName;

    @Column(name = "family_name", nullable = false)
    private String familyName;

    @Column(name = "email", nullable = false)
    private String email;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}
