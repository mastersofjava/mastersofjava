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

import java.time.Instant;
import java.util.UUID;

import javax.persistence.*;

import lombok.*;

@Entity
@Table(name = "test_cases")
@SequenceGenerator(name = "test_cases_id_seq", sequenceName = "test_cases_seq")

@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = { "uuid" })
@ToString(exclude = { "testAttempt" })
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_cases_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "test_attempt_id", nullable = false)
    private TestAttempt testAttempt;

    @Column(name = "date_time_register", nullable = false)
    private Instant dateTimeRegister;

    @Column(name = "worker", columnDefinition = "TEXT")
    private String worker;

    @Column(name = "trace", columnDefinition = "TEXT")
    private String trace;

    @Column(name = "date_time_start")
    private Instant dateTimeStart;

    @Column(name = "date_time_end")
    private Instant dateTimeEnd;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "timeout")
    private Boolean timeout;

    @Column(name = "aborted")
    private Boolean aborted;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "test_output", columnDefinition = "TEXT")
    private String testOutput;

}
