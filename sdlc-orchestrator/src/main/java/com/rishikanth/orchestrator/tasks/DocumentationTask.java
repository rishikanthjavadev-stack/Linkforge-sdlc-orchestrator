package com.rishikanth.orchestrator.tasks;

import com.rishikanth.orchestrator.model.*;
import com.rishikanth.orchestrator.policy.PathGuardrail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DocumentationTask implements StageTask {

    private final Path targetRepoRoot;

    public DocumentationTask(@Value("${orchestrator.target-repo}") String targetRepo) {
        this.targetRepoRoot = Path.of(targetRepo).toAbsolutePath().normalize();
    }

    @Override
    public TaskResult execute(WorkflowInstance instance, NodeDefinition definition) {
        try {
            Path docsDir = PathGuardrail.enforce(targetRepoRoot, targetRepoRoot.resolve("docs/generated"));
            Files.createDirectories(docsDir);
            Path file = PathGuardrail.enforce(targetRepoRoot,
                    docsDir.resolve(instance.getId() + "-change-summary.md"));

            String content = buildSummary(instance);
            Files.writeString(file, content);

            String output = "Wrote change summary to " + file;
            instance.getContext().put("documentationOutput", output);
            instance.getContext().recordDecision(Stage.DOCUMENTATION, output,
                    "Auto-generated from the requirement/design/implementation decisions already " +
                    "recorded in this workflow's context, not written independently.", Actor.AGENT);
            return TaskResult.success(output);
        } catch (PathGuardrail.PolicyViolationException e) {
            return TaskResult.failure("Policy guardrail blocked write: " + e.getMessage());
        } catch (IOException e) {
            return TaskResult.failure("Documentation write failed: " + e.getMessage());
        }
    }

    private String buildSummary(WorkflowInstance instance) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Change Summary — Workflow ").append(instance.getId()).append("\n\n");
        sb.append("**Scenario:** ").append(instance.getScenarioType()).append("\n\n");
        sb.append("## Decision Lineage\n\n");
        for (DecisionRecord d : instance.getContext().getDecisions()) {
            sb.append("- **[").append(d.getStage()).append(" / ").append(d.getDecidedBy()).append("]** ")
              .append(d.getDecision()).append("\n  - _Rationale:_ ").append(d.getRationale()).append("\n");
        }
        return sb.toString();
    }
}
