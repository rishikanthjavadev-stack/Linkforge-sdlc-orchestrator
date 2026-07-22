package com.rishikanth.orchestrator.model;

import java.util.Set;

/**
 * Immutable definition of a node in the workflow DAG. This is the template;
 * runtime state lives in NodeState. Kept separate so the same graph shape
 * can be inspected/reasoned about before any execution starts.
 */
public class NodeDefinition {
    private final String id;
    private final Stage stage;
    private final Set<String> dependsOn;
    private final boolean requiresApproval;   // high-impact action gate (human)
    private final boolean critical;           // if true, exhausting retries safe-stops the whole workflow
    private final int maxRetries;

    public NodeDefinition(String id, Stage stage, Set<String> dependsOn,
                           boolean requiresApproval, boolean critical, int maxRetries) {
        this.id = id;
        this.stage = stage;
        this.dependsOn = dependsOn;
        this.requiresApproval = requiresApproval;
        this.critical = critical;
        this.maxRetries = maxRetries;
    }

    public String getId() { return id; }
    public Stage getStage() { return stage; }
    public Set<String> getDependsOn() { return dependsOn; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public boolean isCritical() { return critical; }
    public int getMaxRetries() { return maxRetries; }
}
