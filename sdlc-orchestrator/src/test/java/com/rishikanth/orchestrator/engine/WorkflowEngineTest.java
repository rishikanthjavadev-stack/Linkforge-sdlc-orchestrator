package com.rishikanth.orchestrator.engine;

import com.rishikanth.orchestrator.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WorkflowEngineTest {

    @TempDir
    static Path tempRepo;

    @DynamicPropertySource
    static void overrideTargetRepo(DynamicPropertyRegistry registry) {
        registry.add("orchestrator.target-repo", () -> tempRepo.toString());
        registry.add("orchestrator.retry-backoff-ms", () -> "50"); // fast retries for tests
    }

    @Autowired
    private WorkflowEngine engine;

    @BeforeAll
    static void setUpFakeGreenfieldArtifacts() throws IOException {
        // Deliberately create only SOME of the required greenfield files so we can
        // observe the critical-node failure -> SAFE_STOPPED path for that scenario.
        Path modelDir = tempRepo.resolve("src/main/java/com/rishikanth/linkforge/model");
        Files.createDirectories(modelDir);
        Files.writeString(modelDir.resolve("ShortUrl.java"), "// stub");
    }

    private void waitUntil(Supplier<Boolean> condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Condition not met within " + timeoutMs + "ms");
    }

    @Test
    void greenfieldScenario_failsFastOnMissingArtifacts_andSafeStops() throws InterruptedException {
        WorkflowInstance instance = engine.startWorkflow(ScenarioType.GREENFIELD,
                "Build a URL shortener from scratch", false);

        waitUntil(() -> engine.get(instance.getId()).getStatus() == WorkflowStatus.SAFE_STOPPED, 5000);

        WorkflowInstance result = engine.get(instance.getId());
        assertThat(result.getNodeStates().get("requirements").getStatus()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(result.getNodeStates().get("design").getStatus()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(result.getNodeStates().get("implementation").getStatus()).isEqualTo(NodeStatus.FAILED);
        // downstream nodes never reached since implementation is critical and a hard dependency
        assertThat(result.getNodeStates().get("release").getStatus()).isEqualTo(NodeStatus.PENDING);
    }

    @Test
    void ambiguousScenario_blocksForClarification_thenReplansAndReachesApprovalGate() throws InterruptedException {
        WorkflowInstance instance = engine.startWorkflow(ScenarioType.AMBIGUOUS,
                "Make the shortener more reliable", false);

        waitUntil(() -> engine.get(instance.getId()).getNodeStates().get("requirements").getStatus()
                == NodeStatus.BLOCKED_CLARIFICATION, 5000);
        assertThat(engine.get(instance.getId()).getStatus()).isEqualTo(WorkflowStatus.AWAITING_HUMAN);

        // Human resolves the ambiguity -> re-planning: requirements re-executes, cascades forward
        engine.clarify(instance.getId(), "requirements",
                "Add per-IP rate limiting to protect against traffic abuse");

        waitUntil(() -> engine.get(instance.getId()).getNodeStates().get("release").getStatus()
                == NodeStatus.AWAITING_APPROVAL, 8000);

        WorkflowInstance result = engine.get(instance.getId());
        assertThat(result.getNodeStates().get("implementation").getStatus()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(result.getContext().get("generatedFiles")).isNotNull();
        assertThat(Files.exists(tempRepo.resolve(
                "src/main/java/com/rishikanth/linkforge/filter/RateLimitFilter.java"))).isTrue();

        // Human approves the high-impact release action
        engine.approve(instance.getId(), "release", "Reviewed generated filter, looks correct");
        waitUntil(() -> engine.get(instance.getId()).getStatus() == WorkflowStatus.COMPLETED, 5000);

        assertThat(Files.exists(tempRepo.resolve(
                "docs/generated/" + instance.getId() + "-release-manifest.json"))).isTrue();
    }

    @Test
    void brownfieldScenario_withSimulatedReleaseFailure_rollsBackManifest() throws InterruptedException {
        WorkflowInstance instance = engine.startWorkflow(ScenarioType.BROWNFIELD,
                "Add rate limiting to protect against abuse", true);

        waitUntil(() -> engine.get(instance.getId()).getNodeStates().get("release").getStatus()
                == NodeStatus.AWAITING_APPROVAL, 8000);

        engine.approve(instance.getId(), "release", "Approving to exercise rollback demo path");

        waitUntil(() -> engine.get(instance.getId()).getStatus() == WorkflowStatus.SAFE_STOPPED, 5000);

        WorkflowInstance result = engine.get(instance.getId());
        assertThat(result.getNodeStates().get("release").getStatus()).isEqualTo(NodeStatus.ROLLED_BACK);
        assertThat(Files.exists(tempRepo.resolve(
                "docs/generated/" + instance.getId() + "-release-manifest.json"))).isFalse();
    }
}
