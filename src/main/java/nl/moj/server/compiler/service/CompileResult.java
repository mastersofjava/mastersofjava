package nl.moj.server.compiler.service;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CompileResult {

    private UUID compileAttemptUuid;
    private boolean success;
    private String compileOutput;

}
