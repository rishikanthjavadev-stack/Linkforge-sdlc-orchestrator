package com.rishikanth.orchestrator.tasks;

import com.rishikanth.orchestrator.model.*;
import org.springframework.stereotype.Component;

@Component
public class DesignTask implements StageTask {

    @Override
    public TaskResult execute(WorkflowInstance instance, NodeDefinition definition) {
        String normalizedRequirement = instance.getContext().get("normalizedRequirement");
        if (normalizedRequirement == null) {
            return TaskResult.failure("DESIGN cannot proceed: no normalized requirement in context " +
                    "(REQUIREMENTS stage must complete first - dependency graph should have prevented this)");
        }

        String design = switch (instance.getScenarioType()) {
            case GREENFIELD -> "Base62(auto-increment id) for short codes (no collision retries needed); " +
                    "denormalized click_count/last_accessed_at on the short_urls row for O(1) analytics reads; " +
                    "H2 file-mode DB for prototype, schema is Postgres-portable.";
            case BROWNFIELD, AMBIGUOUS -> "In-memory per-IP token bucket implemented as a servlet Filter, " +
                    "scoped only to /api/shorten and /{code}. No changes to ShortUrlService, ShortUrlRepository, " +
                    "or the ShortUrl entity - rate limiting is cross-cutting and stays out of domain logic.";
        };

        instance.getContext().put("designDecision", design);
        instance.getContext().recordDecision(Stage.DESIGN, design,
                "Chosen to minimize blast radius on existing, already-verified code paths.", Actor.AGENT);

        return TaskResult.success(design);
    }
}
