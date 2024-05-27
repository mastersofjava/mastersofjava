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
package nl.moj.server.compiler.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.messages.JMSCompileRequest;
import nl.moj.common.messages.JMSCompileResponse;
import nl.moj.common.messages.JMSFile;
import nl.moj.server.assignment.service.AssignmentService;
import nl.moj.server.compiler.model.CompileAttempt;
import nl.moj.server.compiler.repository.CompileAttemptRepository;
import nl.moj.server.message.service.MessageService;
import nl.moj.server.metrics.MetricsService;
import nl.moj.server.runtime.model.TeamAssignmentStatus;
import nl.moj.server.runtime.repository.TeamAssignmentStatusRepository;
import nl.moj.server.teams.service.TeamService;
import nl.moj.server.util.JMSResponseHelper;
import nl.moj.server.util.TransactionHelper;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompileService {

    private final CompileAttemptRepository compileAttemptRepository;
    private final TeamAssignmentStatusRepository teamAssignmentStatusRepository;
    private final TeamService teamService;
    private final JmsTemplate jmsTemplate;
    private final MessageService messageService;
    private final JMSResponseHelper responseHelper;
    private final AssignmentService assignmentService;
    private final TaskScheduler taskScheduler;
    private final TransactionHelper trx;
    private final MetricsService metricsService;

    @Transactional
    public void receiveCompileResponse(JMSCompileResponse compileResponse) {
        log.info("Received compile attempt response {}", compileResponse.getAttempt());
        CompileAttempt compileAttempt = compileAttemptRepository.findByUuid(compileResponse.getAttempt());

        if (compileAttempt.getDateTimeEnd() == null) {
            compileAttempt = update(compileAttempt, compileResponse);

            // if this is the most recent compileAttempt, send response to client else, skip.
            if (isMostRecent(compileAttempt)) {
                messageService.sendCompileFeedback(compileAttempt);
            } else {
                log.info("Ignoring compile feedback for compile attempt {}, already a newer compile attempt pending.",
                        compileAttempt.getUuid());
            }
            registerCompileAttemptMetrics(compileAttempt);
        } else {
            log.info("Ignoring response for compile attempt {}, already have a response.", compileAttempt.getUuid());
        }
    }

    private boolean isMostRecent(CompileAttempt compileAttempt) {
        return compileAttemptRepository.countNewerAttempts(compileAttempt.getAssignmentStatus(),
                compileAttempt.getDateTimeRegister()) == 0;
    }

    private void registerCompileAttemptMetrics(CompileAttempt compileAttempt) {
        metricsService.registerCompileAttemptMetrics(compileAttempt);
    }

    @Transactional
    @NewSpan
    public CompileAttempt registerCompileAttempt(CompileRequest compileRequest) throws CompileAttemptRegisterException {
        log.info("Registering compile attempt for assignment {} by team {}.", compileRequest.getAssignment().getUuid(),
                compileRequest.getTeam().getUuid());

        try {
            // save the team progress
            teamService.updateAssignment(compileRequest.getTeam().getUuid(), compileRequest.getSession().getUuid(),
                    compileRequest.getAssignment().getUuid(), compileRequest.getSources());

            CompileAttempt compileAttempt = prepareCompileAttempt(compileRequest);
            // send JMS compile request
            jmsTemplate.convertAndSend("operation_request", JMSCompileRequest.builder()
                    .attempt(compileAttempt.getUuid())
                    .assignment(compileRequest.getAssignment().getUuid())
                    .sources(compileRequest.getSources().entrySet().stream().map(e -> JMSFile.builder()
                            .type(JMSFile.Type.SOURCE)
                            .path(e.getKey().toString())
                            .content(e.getValue())
                            .build()).collect(Collectors.toList()))
                    .build());

            // schedule controller abort
            scheduleAbort(compileAttempt);

            messageService.sendCompilingStarted(compileRequest.getTeam());

            log.info("Compile attempt {} for assignment {} by team {} registered.", compileAttempt.getUuid(),
                    compileRequest.getAssignment()
                            .getUuid(),
                    compileRequest.getTeam().getUuid());
            return compileAttempt;
        } catch (IOException e) {
            messageService.sendCompileUnprocessable(compileRequest.getTeam());
            throw new CompileAttemptRegisterException(String
                    .format("Failed to register compile attempt for assignment %s by team %s.", compileRequest.getAssignment()
                            .getUuid(), compileRequest.getTeam().getUuid()),
                    e);
        }
    }

    @Transactional
    public CompileAttempt prepareCompileAttempt(CompileRequest compileRequest) {
        TeamAssignmentStatus as = teamAssignmentStatusRepository.findByAssignment_IdAndCompetitionSession_IdAndTeam_Id(
                compileRequest.getAssignment().getId(), compileRequest.getSession().getId(),
                compileRequest.getTeam().getId());

        CompileAttempt compileAttempt = CompileAttempt.builder()
                .assignmentStatus(as)
                .uuid(UUID.randomUUID())
                .dateTimeRegister(Instant.now())
                .build();
        as.getCompileAttempts().add(compileAttempt);
        return compileAttemptRepository.save(compileAttempt);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public CompileAttempt update(CompileAttempt compileAttempt, JMSCompileResponse compileResponse) {

        if (compileAttempt == null) {
            return null;
        }
        if (compileResponse == null) {
            return compileAttempt;
        }

        compileAttempt.setWorker(compileResponse.getWorker());
        compileAttempt.setTrace(compileResponse.getTraceId());
        compileAttempt.setDateTimeStart(compileResponse.getStarted());
        compileAttempt.setDateTimeEnd(compileResponse.getEnded());
        compileAttempt.setSuccess(compileResponse.isSuccess());
        compileAttempt.setTimeout(compileResponse.isTimeout());
        compileAttempt.setCompilerOutput(compileResponse.getOutput());
        compileAttempt.setAborted(compileResponse.isAborted());
        compileAttempt.setReason(compileResponse.getReason());
        return compileAttemptRepository.save(compileAttempt);
    }

    private void scheduleAbort(CompileAttempt compileAttempt) {
        Duration timeout = assignmentService.resolveCompileAbortTimout(compileAttempt.getAssignmentStatus()
                .getAssignment());
        taskScheduler.schedule(() -> {
            trx.required(() -> {
                CompileAttempt ca = compileAttemptRepository.findByUuid(compileAttempt.getUuid());
                if (ca != null && ca.getDateTimeEnd() == null && ca.getAssignmentStatus().getDateTimeEnd() == null) {
                    log.info("Aborting compile attempt {}, response took too long.", ca.getUuid());
                    receiveCompileResponse(responseHelper.abortResponse(ca));
                }
            });
        }, Instant.now().plus(timeout));
    }
}
