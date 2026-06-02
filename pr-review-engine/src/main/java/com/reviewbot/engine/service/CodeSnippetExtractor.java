package com.reviewbot.engine.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CodeSnippetExtractor {

    private static final Pattern DIFF_FILE_PATTERN = Pattern.compile("^diff --git a/(.*?) b/(.*?)$", Pattern.MULTILINE);
    private static final Pattern MAIN_METHOD_PATTERN = Pattern.compile(
            "public\\s+static\\s+void\\s+main\\s*\\(\\s*String");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
            "public\\s+class\\s+(\\w+)");

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestableSnippet {
        private String filePath;
        private String className;
        private String sourceCode;
    }

    public List<TestableSnippet> extractTestableSnippets(String diff, Path repoDir) {
        List<String> changedJavaFiles = extractChangedJavaFiles(diff);
        List<TestableSnippet> snippets = new ArrayList<>();

        for (String filePath : changedJavaFiles) {
            Path fullPath = repoDir.resolve(filePath);
            if (!Files.exists(fullPath)) {
                log.debug("Changed file not found in repo (possibly deleted): {}", filePath);
                continue;
            }

            try {
                String sourceCode = Files.readString(fullPath);
                if (isTestable(sourceCode)) {
                    String className = extractClassName(sourceCode);
                    if (className != null) {
                        snippets.add(TestableSnippet.builder()
                                .filePath(filePath)
                                .className(className)
                                .sourceCode(sourceCode)
                                .build());
                        log.info("Found testable snippet: {} (class={})", filePath, className);
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to read file: {}", fullPath, e);
            }
        }

        log.info("Extracted {} testable snippets from {} changed Java files",
                snippets.size(), changedJavaFiles.size());
        return snippets;
    }

    List<String> extractChangedJavaFiles(String diff) {
        List<String> javaFiles = new ArrayList<>();
        Matcher matcher = DIFF_FILE_PATTERN.matcher(diff);

        while (matcher.find()) {
            String filePath = matcher.group(2);
            if (filePath.endsWith(".java") && !filePath.contains("test/") && !filePath.contains("Test.java")) {
                javaFiles.add(filePath);
            }
        }
        return javaFiles;
    }

    private boolean isTestable(String sourceCode) {
        if (sourceCode.contains("@SpringBootApplication")) return false;
        if (sourceCode.contains("@Configuration")) return false;
        if (sourceCode.contains("interface ") && !sourceCode.contains("class ")) return false;

        if (MAIN_METHOD_PATTERN.matcher(sourceCode).find()) return true;
        if (isStandaloneClass(sourceCode)) return true;

        return false;
    }

    private boolean isStandaloneClass(String sourceCode) {
        boolean hasClass = sourceCode.contains("class ");
        boolean hasMinimalDeps = !sourceCode.contains("@Autowired")
                && !sourceCode.contains("@Inject")
                && !sourceCode.contains("@Service")
                && !sourceCode.contains("@Component")
                && !sourceCode.contains("@Repository")
                && !sourceCode.contains("@Controller");
        boolean importsAreLimited = countImports(sourceCode) <= 10;

        return hasClass && hasMinimalDeps && importsAreLimited;
    }

    private int countImports(String sourceCode) {
        int count = 0;
        for (String line : sourceCode.split("\n")) {
            if (line.trim().startsWith("import ")) count++;
        }
        return count;
    }

    private String extractClassName(String sourceCode) {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(sourceCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
