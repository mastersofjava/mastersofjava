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
package nl.moj.server.compiler.model;

import lombok.*;
import nl.moj.server.runtime.model.AssignmentStatus;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compile_attempts")
@SequenceGenerator(name = "compile_attempts_id_seq", sequenceName = "compile_attempts_seq")

@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = {"uuid"})
@ToString(exclude = {"assignmentStatus"})
public class CompileAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compile_attempts_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "assignment_status_id", nullable = false)
    private AssignmentStatus assignmentStatus;

    @Column(name = "date_time_register", nullable = false)
    private Instant dateTimeRegister;

    @Column(name = "worker", columnDefinition = "TEXT")
    private String worker;

    @Column(name = "run", columnDefinition = "uuid")
    private UUID run;

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

    @Column(name = "compiler_output", columnDefinition = "TEXT")
    private String compilerOutput;
}
