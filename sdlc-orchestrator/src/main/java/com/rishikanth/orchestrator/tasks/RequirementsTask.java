package com.rishikanth.orchestrator.tasks;

import com.rishikanth.orchestrator.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Interprets the raw requirement text stored in context under "requirementText".
 * For the AMBIGUOUS scenario this deliberately returns TaskResult.ambiguous(...)
 * rather than guessing - the engine turns that into a BLOCKED_CLARIFICATION
 * pause, matching the "human approval/clarification checkpoint" requirement.
 */
@Component
public class RequirementsTask implements StageTask {

    private static final List<String> AMBIGUITY_SIGNALS = List.of(
            "more reliable", "better", "improve", "faster", "more secure", "more scalable"
    );

    @Override
    public TaskResult execute(WorkflowInstance instance, NodeDefinition definition) {
        String raw = instance.getContext().get("requirementText");
        if (raw == null || raw.isBlank()) {
            return TaskResult.failure("No requirement text provided in context");
        }

        boolean alreadyClarified = "true".equals(instance.getContext().get("clarified"));
        String lower = raw.toLowerCase();

        boolean isAmbiguous = instance.getScenarioType() == ScenarioType.AMBIGUOUS
                && !alreadyClarified
                && AMBIGUITY_SIGNALS.stream().anyMatch(lower::contains);

        if (isAmbiguous) {
            return TaskResult.ambiguous(
                    "Requirement '" + raw + "' is a quality attribute with multiple valid " +
                    "engineering interpretations (traffic abuse, data loss, bad input, downstream " +
                    "failure, availability). Human clarification required before proceeding - " +
                    "see docs/02-ambiguous-requirement-normalization.md for the full analysis.");
        }

        String normalized = switch (instance.getScenarioType()) {
            case GREENFIELD -> "Build a URL shortener exposing POST /api/shorten, GET /{code} " +
                    "(redirect), and GET /api/analytics/{code}, with Base62-encoded short codes, " +
                    "configurable expiry, and click-count analytics.";
            case BROWNFIELD, AMBIGUOUS -> "Add per-IP rate limiting to POST /api/shorten and " +
                    "GET /{code} to protect against single-source abuse, returning 429 with Retry-After.";
        };
        instance.getContext().put("normalizedRequirement", normalized);
        instance.getContext().recordDecision(Stage.REQUIREMENTS, normalized,
                alreadyClarified
                        ? "Resolved from ambiguous input after human clarification selected the traffic-abuse interpretation."
                        : "Requirement was specific enough to normalize directly without ambiguity.",
                alreadyClarified ? Actor.HUMAN : Actor.AGENT);

        return TaskResult.success(normalized);
    }
}
