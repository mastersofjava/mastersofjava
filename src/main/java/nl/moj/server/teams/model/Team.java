/*
   Copyright 2020 First Eight BV (The Netherlands)


   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.teams.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.*;

import lombok.*;
import nl.moj.server.user.model.User;

@Entity
@Table(name = "teams")
@SequenceGenerator(name = "team_id_seq", sequenceName = "teams_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = { "uuid" })
@ToString(exclude = "users")
public class Team {

    @Id
    @GeneratedValue(generator = "team_id_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID uuid;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "country")
    private String country;

    @Column(name = "company")
    private String company;

    @Column(name = "indication")
    private String indication;

    @OneToMany(mappedBy = "team")
    @Builder.Default
    private List<User> users = new ArrayList<>();

    public String getShortName() {
        return name.length() > 20 ? name.substring(0, 20) + "..." : name;
    }

    public boolean isDisabled() {
        return !"ARCHIVE".equals(indication) && !"DISQUALIFY".equals(indication);
    }

}
