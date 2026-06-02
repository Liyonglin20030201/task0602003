package com.reviewbot.engine.service;

import com.reviewbot.core.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryCloneService {

    private final AppProperties appProperties;

    public Path cloneOrPull(String repoFullName, String headSha) {
        Path baseDir = Path.of(appProperties.getRepository().getCloneBaseDir());
        Path repoDir = baseDir.resolve(repoFullName.replace("/", "_"));

        try {
            Files.createDirectories(baseDir);

            if (Files.exists(repoDir.resolve(".git"))) {
                log.info("Repository already exists, fetching and checking out: {}", repoFullName);
                executeGit(repoDir, "git", "fetch", "origin");
                executeGit(repoDir, "git", "checkout", headSha);
            } else {
                log.info("Cloning repository: {}", repoFullName);
                String cloneUrl = buildCloneUrl(repoFullName);
                executeGit(baseDir, "git", "clone", "--depth", "50", cloneUrl, repoDir.getFileName().toString());
                executeGit(repoDir, "git", "checkout", headSha);
            }

            log.info("Repository ready at: {}", repoDir);
            return repoDir;

        } catch (Exception e) {
            log.error("Failed to clone/pull repository: {}", repoFullName, e);
            throw new RuntimeException("Repository clone failed: " + e.getMessage(), e);
        }
    }

    private String buildCloneUrl(String repoFullName) {
        String token = appProperties.getGithub().getToken();
        if (token != null && !token.isBlank()) {
            return "https://x-access-token:" + token + "@github.com/" + repoFullName + ".git";
        }
        return "https://github.com/" + repoFullName + ".git";
    }

    private void executeGit(Path workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Git command timed out: " + String.join(" ", command));
        }

        if (process.exitValue() != 0) {
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            throw new RuntimeException("Git command failed (exit=" + process.exitValue() + "): " + output);
        }
    }
}
