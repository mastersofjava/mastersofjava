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

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import nl.moj.server.assignment.model.Assignment;

@Entity
@Table(name = "competition_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "competition_assignments_competition_idx_uk", columnNames = { "idx", "competition_id" })
})
@Data
@NoArgsConstructor(force = true)
@SequenceGenerator(name = "competition_assignments_seq", sequenceName = "competition_assignments_seq")
@EqualsAndHashCode(of = { "id" })
public class CompetitionAssignment {

    @Id
    @GeneratedValue(generator = "competition_assignments_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "idx", nullable = false)
    private Integer order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "competition_id", nullable = false)
    @JsonIgnore
    private Competition competition;

}
