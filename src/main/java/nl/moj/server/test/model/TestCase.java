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
package nl.moj.server.test.model;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

import lombok.*;


@Entity
@Table(name = "test_cases")
@SequenceGenerator(name = "id_seq", sequenceName = "test_cases_seq")

@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = {"uuid"})
@ToString(exclude = {"testAttempt"})
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttempt testAttempt;

    @Column(name = "date_time_start", nullable = false)
    private Instant dateTimeStart;

    @Column(name = "date_time_end", nullable = false)
    private Instant dateTimeEnd;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "timeout", nullable = false)
    private boolean timeout;

    @Column(name = "test_output", columnDefinition = "TEXT")
    private String testOutput;

}
