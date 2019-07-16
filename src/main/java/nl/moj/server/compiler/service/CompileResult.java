package nl.moj.server.compiler.service;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CompileResult {

    private UUID compileAttemptUuid;
    private Instant dateTimeStart;
    private Instant dateTimeEnd;
    private boolean success;
    private String compileOutput;

}
