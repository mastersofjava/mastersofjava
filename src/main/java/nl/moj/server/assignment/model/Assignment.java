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
package nl.moj.server.assignment.model;

import java.time.Duration;
import java.util.UUID;

import javax.persistence.*;

import lombok.*;

/**
 * @author Ejnar Kaekebeke
 * @author Bas Passon
 */
@Entity
@Table(name = "assignments")
@SequenceGenerator(name = "assignment_id_seq", sequenceName = "assignments_seq")

@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = { "uuid" })
public class Assignment {

    @Id
    @GeneratedValue(generator = "assignment_id_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false, columnDefinition = "uuid")
    private UUID uuid;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "collection", nullable = false)
    private String collection;

    /** The file on disk that contains the assignment descriptor (e.g. assignment.yaml) */
    @Column(name = "assignment_descriptor", unique = true, nullable = false)
    private String assignmentDescriptor;

    @Column(name = "assignment_duration", nullable = false)
    private Duration assignmentDuration;

    @Column(name = "allowed_submits", nullable = false)
    private Integer allowedSubmits;

}
