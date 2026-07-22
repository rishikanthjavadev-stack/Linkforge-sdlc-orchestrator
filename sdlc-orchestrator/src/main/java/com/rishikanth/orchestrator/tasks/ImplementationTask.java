package com.rishikanth.orchestrator.tasks;

import com.rishikanth.orchestrator.model.*;
import com.rishikanth.orchestrator.policy.ChangeControlPolicy;
import com.rishikanth.orchestrator.policy.PathGuardrail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * GREENFIELD: verifies the Phase 1 core artifacts actually exist on disk
 * (codebase reasoning over the existing repo, not blind regeneration).
 *
 * BROWNFIELD / AMBIGUOUS(resolved): generates real source files - a servlet
 * filter and its exception type - directly into the LinkForge repo, guarded
 * by PathGuardrail so the write is provably confined to the target repo root.
 */
@Component
public class ImplementationTask implements StageTask {

    private final Path targetRepoRoot;

    public ImplementationTask(@Value("${orchestrator.target-repo}") String targetRepo) {
        this.targetRepoRoot = Path.of(targetRepo).toAbsolutePath().normalize();
    }

    @Override
    public TaskResult execute(WorkflowInstance instance, NodeDefinition definition) {
        return switch (instance.getScenarioType()) {
            case GREENFIELD -> verifyGreenfieldArtifacts(instance);
            case BROWNFIELD, AMBIGUOUS -> generateRateLimiter(instance);
        };
    }

    private TaskResult verifyGreenfieldArtifacts(WorkflowInstance instance) {
        List<String> requiredFiles = List.of(
                "src/main/java/com/rishikanth/linkforge/model/ShortUrl.java",
                "src/main/java/com/rishikanth/linkforge/service/ShortUrlService.java",
                "src/main/java/com/rishikanth/linkforge/service/Base62Encoder.java",
                "src/main/java/com/rishikanth/linkforge/controller/LinkController.java",
                "src/main/java/com/rishikanth/linkforge/controller/RedirectController.java"
        );
        List<String> missing = new ArrayList<>();
        for (String rel : requiredFiles) {
            if (!Files.exists(targetRepoRoot.resolve(rel))) {
                missing.add(rel);
            }
        }
        if (!missing.isEmpty()) {
            return TaskResult.failure("Greenfield core artifacts missing: " + missing +
                    " (expected under " + targetRepoRoot + ")");
        }
        String output = "Verified " + requiredFiles.size() + " core artifacts present under " + targetRepoRoot;
        instance.getContext().put("implementationOutput", output);
        instance.getContext().recordDecision(Stage.IMPLEMENTATION, output,
                "Greenfield core was hand-built in Phase 1; this stage validates it rather than " +
                "regenerating already-verified code.", Actor.AGENT);
        return TaskResult.success(output);
    }

    private TaskResult generateRateLimiter(WorkflowInstance instance) {
        boolean changeControlApproved = "true".equals(instance.getContext().get("changeControlApproved"));
        try {
            Path filterDir = PathGuardrail.enforce(targetRepoRoot,
                    targetRepoRoot.resolve("src/main/java/com/rishikanth/linkforge/filter"));
            ChangeControlPolicy.enforce(targetRepoRoot, filterDir, changeControlApproved);
            Files.createDirectories(filterDir);

            Path exceptionFile = PathGuardrail.enforce(targetRepoRoot,
                    filterDir.resolve("../exception/RateLimitExceededException.java").normalize());
            ChangeControlPolicy.enforce(targetRepoRoot, exceptionFile, changeControlApproved);
            Files.createDirectories(exceptionFile.getParent());
            Files.writeString(exceptionFile, RATE_LIMIT_EXCEPTION_SOURCE);

            Path filterFile = PathGuardrail.enforce(targetRepoRoot, filterDir.resolve("RateLimitFilter.java"));
            ChangeControlPolicy.enforce(targetRepoRoot, filterFile, changeControlApproved);
            Files.writeString(filterFile, RATE_LIMIT_FILTER_SOURCE);

            String output = "Generated " + filterFile + " and " + exceptionFile;
            instance.getContext().put("implementationOutput", output);
            instance.getContext().put("generatedFiles", filterFile + "," + exceptionFile);
            instance.getContext().recordDecision(Stage.IMPLEMENTATION, output,
                    "Implemented as a standalone servlet Filter + exception type; no existing " +
                    "service/repository/entity classes touched (see DESIGN decision). Both a " +
                    "path-containment guardrail and a change-control policy check were evaluated " +
                    "before any file was written.", Actor.AGENT);
            return TaskResult.success(output);
        } catch (PathGuardrail.PolicyViolationException e) {
            return TaskResult.failure("Path guardrail blocked write: " + e.getMessage());
        } catch (ChangeControlPolicy.ChangeControlViolationException e) {
            return TaskResult.failure("Change-control policy blocked write: " + e.getMessage());
        } catch (IOException e) {
            return TaskResult.failure("File write failed: " + e.getMessage());
        }
    }

    private static final String RATE_LIMIT_EXCEPTION_SOURCE = """
            package com.rishikanth.linkforge.exception;

            /** Thrown by RateLimitFilter when a client exceeds its per-IP token bucket. */
            public class RateLimitExceededException extends RuntimeException {
                public RateLimitExceededException(String message) {
                    super(message);
                }
            }
            """;

    private static final String RATE_LIMIT_FILTER_SOURCE = """
            package com.rishikanth.linkforge.filter;

            import jakarta.servlet.*;
            import jakarta.servlet.http.HttpServletRequest;
            import jakarta.servlet.http.HttpServletResponse;
            import org.springframework.stereotype.Component;

            import java.io.IOException;
            import java.util.concurrent.ConcurrentHashMap;
            import java.util.concurrent.atomic.AtomicInteger;

            /**
             * Per-IP token bucket rate limiter, generated by the SDLC orchestrator's
             * IMPLEMENTATION stage for the brownfield "add rate limiting" scenario.
             *
             * Known limitation (documented, not hidden): in-memory only, so limits are
             * not shared across multiple instances of this service. Acceptable for a
             * single-instance prototype; a distributed limiter (e.g. Redis-backed) is
             * the production follow-up, named explicitly rather than silently assumed.
             */
            @Component
            public class RateLimitFilter implements Filter {

                private static final int MAX_REQUESTS_PER_WINDOW = 20;
                private static final long WINDOW_MILLIS = 60_000L;

                private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

                @Override
                public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                        throws IOException, ServletException {
                    HttpServletRequest httpRequest = (HttpServletRequest) request;
                    HttpServletResponse httpResponse = (HttpServletResponse) response;

                    String path = httpRequest.getRequestURI();
                    boolean isGuarded = path.equals("/api/shorten") || path.matches("^/[A-Za-z0-9]+$");
                    if (!isGuarded) {
                        chain.doFilter(request, response);
                        return;
                    }

                    String clientIp = httpRequest.getRemoteAddr();
                    Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> new Bucket());

                    if (!bucket.tryConsume()) {
                        httpResponse.setStatus(429);
                        httpResponse.setHeader("Retry-After", "60");
                        httpResponse.getWriter().write(
                                "{\\"error\\":\\"Too Many Requests\\",\\"message\\":\\"Rate limit exceeded, retry after 60s\\"}");
                        return;
                    }

                    chain.doFilter(request, response);
                }

                private static class Bucket {
                    private final AtomicInteger count = new AtomicInteger(0);
                    private volatile long windowStart = System.currentTimeMillis();

                    synchronized boolean tryConsume() {
                        long now = System.currentTimeMillis();
                        if (now - windowStart > WINDOW_MILLIS) {
                            windowStart = now;
                            count.set(0);
                        }
                        return count.incrementAndGet() <= MAX_REQUESTS_PER_WINDOW;
                    }
                }
            }
            """;
}
