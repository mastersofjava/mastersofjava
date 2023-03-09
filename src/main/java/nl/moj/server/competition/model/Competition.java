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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "competitions")
@NoArgsConstructor(force = true)
@SequenceGenerator(name = "competitions_seq", sequenceName = "competitions_seq")
@EqualsAndHashCode(of = {"uuid"})
public class Competition {

    @Id
    @GeneratedValue(generator = "competitions_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false, columnDefinition = "uuid")
    private UUID uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompetitionAssignment> assignments = new ArrayList<>();

    @JsonIgnore
    public List<CompetitionAssignment> getAssignmentsInOrder() {
        List<CompetitionAssignment> copyFiltered = copyFilteredList();
        copyFiltered.sort(Comparator.comparingInt(CompetitionAssignment::getOrder));
        return copyFiltered;
    }

    private List<CompetitionAssignment> copyFilteredList() {
        boolean isDefault = !name.contains("|20");
        if (isDefault) {
            return new ArrayList<>(assignments);
        }
        String collectionName = name.split("\\|")[1];
        return assignments.stream()
                .filter(orderedAssignment -> orderedAssignment.getAssignment()
                        .getAssignmentDescriptor()
                        .contains(collectionName))
                .collect(Collectors.toList());
    }

    public String getShortName() {
        return name.split("\\|")[0];
    }

    public String getDisplayName() {
        if (!name.contains("|20")) {
            return name;
        }
        String[] parts = name.split("\\|");
        return parts[0] + " (" + parts[1] + ")";
    }
}
