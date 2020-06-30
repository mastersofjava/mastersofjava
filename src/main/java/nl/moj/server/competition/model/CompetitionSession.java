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
package nl.moj.server.competition.model;


import javax.persistence.*;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "competition_sessions")
@NoArgsConstructor(force = true)
@SequenceGenerator(name = "competition_sessions_seq", sequenceName = "competition_sessions_seq")
public class CompetitionSession {

    @Id
    @GeneratedValue(generator = "competition_sessions_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid;

    @Column(name = "active")
    private boolean active;

    @ManyToOne
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;
}
