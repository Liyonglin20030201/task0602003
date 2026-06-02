package com.reviewbot.engine.client;

import com.reviewbot.core.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GitHubApiClient {

    private final WebClient webClient;
    private final AppProperties.GitHub githubConfig;

    public GitHubApiClient(AppProperties appProperties) {
        this.githubConfig = appProperties.getGithub();
        this.webClient = WebClient.builder()
                .baseUrl(githubConfig.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubConfig.getToken())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String fetchDiff(String repoFullName, int prNumber) {
        return webClient.get()
                .uri("/repos/{repo}/pulls/{pr}", repoFullName, prNumber)
                .header(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));
    }

    public void postReviewComment(String repoFullName, int prNumber, String commitSha, String body,
                                  List<Map<String, Object>> comments) {
        Map<String, Object> reviewPayload = Map.of(
                "commit_id", commitSha,
                "body", body,
                "event", "COMMENT",
                "comments", comments
        );

        webClient.post()
                .uri("/repos/{repo}/pulls/{pr}/reviews", repoFullName, prNumber)
                .bodyValue(reviewPayload)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        log.info("Posted review to {}#{}", repoFullName, prNumber);
    }

    public void postIssueComment(String repoFullName, int prNumber, String body) {
        Map<String, String> payload = Map.of("body", body);

        webClient.post()
                .uri("/repos/{repo}/issues/{pr}/comments", repoFullName, prNumber)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(30));

        log.info("Posted issue comment to {}#{}", repoFullName, prNumber);
    }
}
