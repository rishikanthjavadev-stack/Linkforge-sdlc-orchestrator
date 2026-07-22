package com.rishikanth.orchestrator.tasks;

import com.rishikanth.orchestrator.model.*;
import com.rishikanth.orchestrator.policy.PathGuardrail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * By the time this task runs, the engine has already required and recorded
 * human approval (see WorkflowEngine.approve) - this task only executes the
 * mechanics of "release": writing a manifest recording what was released.
 *
 * A workflow can be started with simulateReleaseFailure=true purely to
 * exercise the rollback path in a demo; this is called out explicitly as a
 * test hook, not a hidden behavior.
 */
@Component
public class ReleaseTask implements StageTask {

    private final Path targetRepoRoot;

    public ReleaseTask(@Value("${orchestrator.target-repo}") String targetRepo) {
        this.targetRepoRoot = Path.of(targetRepo).toAbsolutePath().normalize();
    }

    @Override
    public TaskResult execute(WorkflowInstance instance, NodeDefinition definition) {
        if ("true".equals(instance.getContext().get("simulateReleaseFailure"))) {
            return TaskResult.failure("Simulated release failure (demo hook: simulateReleaseFailure=true) " +
                    "- used to exercise the rollback control path.");
        }

        try {
            Path manifestPath = PathGuardrail.enforce(targetRepoRoot,
                    targetRepoRoot.resolve("docs/generated/" + instance.getId() + "-release-manifest.json"));
            Files.createDirectories(manifestPath.getParent());

            String manifest = """
                    {
                      "workflowId": "%s",
                      "scenario": "%s",
                      "releasedAt": "%s",
                      "normalizedRequirement": "%s"
                    }
                    """.formatted(instance.getId(), instance.getScenarioType(), Instant.now(),
                    escape(instance.getContext().get("normalizedRequirement")));

            Files.writeString(manifestPath, manifest);

            String output = "Release manifest written to " + manifestPath;
            instance.getContext().put("releaseOutput", output);
            instance.getContext().recordDecision(Stage.RELEASE, output,
                    "Released only after explicit human approval recorded in the audit log.", Actor.HUMAN);
            return TaskResult.success(output);
        } catch (PathGuardrail.PolicyViolationException e) {
            return TaskResult.failure("Policy guardrail blocked write: " + e.getMessage());
        } catch (IOException e) {
            return TaskResult.failure("Release manifest write failed: " + e.getMessage());
        }
    }

    /** Rolls back a completed release by deleting its manifest. Called by the engine, not by tasks themselves. */
    public void rollback(WorkflowInstance instance) {
        try {
            Path manifestPath = targetRepoRoot.resolve("docs/generated/" + instance.getId() + "-release-manifest.json");
            Files.deleteIfExists(manifestPath);
            instance.getContext().recordDecision(Stage.RELEASE,
                    "Rolled back release manifest " + manifestPath,
                    "Post-approval release task failed; rollback reverts the manifest write to leave " +
                    "no partial release artifact behind.", Actor.SYSTEM);
        } catch (IOException ignored) {
            // best-effort rollback; nothing further to revert for this simple artifact
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
