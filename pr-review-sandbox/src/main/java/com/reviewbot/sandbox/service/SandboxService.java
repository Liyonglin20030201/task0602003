package com.reviewbot.sandbox.service;

import com.reviewbot.core.config.AppProperties;
import com.reviewbot.core.model.SandboxResult;
import com.reviewbot.sandbox.model.ExecutionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxService {

    private final ContainerManager containerManager;
    private final AppProperties appProperties;

    public SandboxResult execute(String code) {
        return execute(code, "Main");
    }

    public SandboxResult execute(String code, String className) {
        AppProperties.Sandbox config = appProperties.getSandbox();

        if (!config.isEnabled()) {
            log.debug("Sandbox is disabled, skipping execution");
            return SandboxResult.builder()
                    .exitCode(0)
                    .stdout("Sandbox disabled")
                    .stderr("")
                    .timedOut(false)
                    .executionTimeMs(0)
                    .build();
        }

        ExecutionRequest request = ExecutionRequest.builder()
                .code(code)
                .className(className)
                .timeoutSeconds(config.getTimeoutSeconds())
                .build();

        log.info("Executing code in sandbox (class={}, timeout={}s)", className, config.getTimeoutSeconds());

        return containerManager.execute(
                config.getImageName(),
                request,
                config.getMemoryLimitMb(),
                config.getCpuQuota());
    }
}
