package com.rishikanth.orchestrator.model;

import java.time.Instant;

/**
 * Captures WHY a decision was made at a given stage, not just that a
 * transition happened. This is what "preserve cross-stage context and
 * decision lineage" means concretely: later stages can read prior
 * DecisionRecords out of the WorkflowContext instead of re-deriving
 * assumptions the requirements/design stage already settled.
 */
public class DecisionRecord {
    private final Instant timestamp = Instant.now();
    private final Stage stage;
    private final String decision;
    private final String rationale;
    private final Actor decidedBy;

    public DecisionRecord(Stage stage, String decision, String rationale, Actor decidedBy) {
        this.stage = stage;
        this.decision = decision;
        this.rationale = rationale;
        this.decidedBy = decidedBy;
    }

    public Instant getTimestamp() { return timestamp; }
    public Stage getStage() { return stage; }
    public String getDecision() { return decision; }
    public String getRationale() { return rationale; }
    public Actor getDecidedBy() { return decidedBy; }
}
