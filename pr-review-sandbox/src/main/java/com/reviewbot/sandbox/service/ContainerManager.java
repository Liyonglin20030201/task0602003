package com.reviewbot.sandbox.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.HostConfig;
import com.reviewbot.core.model.SandboxResult;
import com.reviewbot.sandbox.model.ExecutionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerManager {

    private final DockerClient dockerClient;

    public SandboxResult execute(String imageName, ExecutionRequest request, int memoryLimitMb, long cpuQuota) {
        String containerId = null;
        long startTime = System.currentTimeMillis();

        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory((long) memoryLimitMb * 1024 * 1024)
                    .withMemorySwap((long) memoryLimitMb * 1024 * 1024)
                    .withCpuQuota(cpuQuota)
                    .withNetworkMode("none")
                    .withReadonlyRootfs(false);

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withHostConfig(hostConfig)
                    .withEnv("TIMEOUT=" + request.getTimeoutSeconds())
                    .withStdinOpen(false)
                    .withTty(false)
                    .exec();

            containerId = container.getId();

            String fileName = (request.getClassName() != null ? request.getClassName() : "Main") + ".java";
            byte[] codeBytes = request.getCode().getBytes(StandardCharsets.UTF_8);

            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withRemotePath("/sandbox/code")
                    .withTarInputStream(createTarStream(fileName, codeBytes))
                    .exec();

            dockerClient.startContainerCmd(containerId).exec();

            boolean finished = dockerClient.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion(request.getTimeoutSeconds(), TimeUnit.SECONDS);

            long executionTime = System.currentTimeMillis() - startTime;

            if (!finished) {
                dockerClient.stopContainerCmd(containerId).withTimeout(1).exec();
                return SandboxResult.builder()
                        .exitCode(-1)
                        .stdout("")
                        .stderr("Execution timed out after " + request.getTimeoutSeconds() + "s")
                        .timedOut(true)
                        .executionTimeMs(executionTime)
                        .build();
            }

            String logs = collectLogs(containerId);
            int exitCode = dockerClient.inspectContainerCmd(containerId).exec()
                    .getState().getExitCodeLong().intValue();

            return SandboxResult.builder()
                    .exitCode(exitCode)
                    .stdout(logs)
                    .stderr("")
                    .timedOut(false)
                    .executionTimeMs(executionTime)
                    .build();

        } catch (Exception e) {
            log.error("Sandbox execution failed", e);
            return SandboxResult.builder()
                    .exitCode(-1)
                    .stdout("")
                    .stderr("Sandbox error: " + e.getMessage())
                    .timedOut(false)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    log.warn("Failed to remove container {}", containerId, e);
                }
            }
        }
    }

    private String collectLogs(String containerId) {
        StringBuilder sb = new StringBuilder();
        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(false)
                    .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                        @Override
                        public void onNext(com.github.dockerjava.api.model.Frame frame) {
                            sb.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                        }
                    }).awaitCompletion(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return sb.toString();
    }

    private ByteArrayInputStream createTarStream(String fileName, byte[] content) {
        // Simple TAR archive creation for single file
        byte[] header = new byte[512];
        byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, header, 0, Math.min(nameBytes.length, 100));

        // File mode (0644)
        System.arraycopy("0000644\0".getBytes(), 0, header, 100, 8);
        // UID
        System.arraycopy("0001000\0".getBytes(), 0, header, 108, 8);
        // GID
        System.arraycopy("0001000\0".getBytes(), 0, header, 116, 8);
        // Size in octal
        String sizeOctal = String.format("%011o\0", content.length);
        System.arraycopy(sizeOctal.getBytes(), 0, header, 124, 12);
        // Modification time
        String mtime = String.format("%011o\0", System.currentTimeMillis() / 1000);
        System.arraycopy(mtime.getBytes(), 0, header, 136, 12);
        // Checksum placeholder
        System.arraycopy("        ".getBytes(), 0, header, 148, 8);
        // Type flag (regular file)
        header[156] = '0';

        // Calculate checksum
        int checksum = 0;
        for (byte b : header) {
            checksum += (b & 0xFF);
        }
        String checksumStr = String.format("%06o\0 ", checksum);
        System.arraycopy(checksumStr.getBytes(), 0, header, 148, 8);

        // Padding to 512-byte boundary
        int padding = (512 - (content.length % 512)) % 512;
        byte[] result = new byte[512 + content.length + padding + 1024];
        System.arraycopy(header, 0, result, 0, 512);
        System.arraycopy(content, 0, result, 512, content.length);

        return new ByteArrayInputStream(result);
    }
}
