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

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "competitions")
@NoArgsConstructor(force = true)
@SequenceGenerator(name = "competitions_seq", sequenceName = "competitions_seq")
public class Competition {

    @Id
    @GeneratedValue(generator = "competitions_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "competition", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<OrderedAssignment> assignments = new ArrayList<>();

    public List<OrderedAssignment> getAssignmentsInOrder() {
        List<OrderedAssignment> copyFiltered = copyFilteredList();
        copyFiltered.sort(Comparator.comparingInt(OrderedAssignment::getOrder));
        return copyFiltered;
    }

    private List<OrderedAssignment> copyFilteredList() {
        boolean isDefault = !name.startsWith("20");
        if (isDefault) {
            return new ArrayList<>(assignments);
        }
        List<OrderedAssignment> filteredList = new ArrayList<>();
        for (OrderedAssignment oa: assignments) {
            if (oa.getAssignment().getAssignmentDescriptor().contains(name)) {
                filteredList.add(oa);
            }
        }
        return filteredList;
    }
}
