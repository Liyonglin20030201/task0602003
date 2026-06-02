package com.reviewbot.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private GitHub github = new GitHub();
    private Claude claude = new Claude();
    private Sandbox sandbox = new Sandbox();
    private Repository repository = new Repository();

    @Data
    public static class GitHub {
        private String webhookSecret;
        private String token;
        private String apiUrl = "https://api.github.com";
    }

    @Data
    public static class Claude {
        private String apiKey;
        private String model = "claude-sonnet-4-20250514";
        private int maxTokens = 4096;
        private String baseUrl = "https://api.anthropic.com/v1";
    }

    @Data
    public static class Sandbox {
        private boolean enabled = true;
        private String imageName = "pr-review-sandbox-java17";
        private int timeoutSeconds = 30;
        private int memoryLimitMb = 256;
        private long cpuQuota = 50000;
    }

    @Data
    public static class Repository {
        private String cloneBaseDir = "./data/repos";
    }
}
