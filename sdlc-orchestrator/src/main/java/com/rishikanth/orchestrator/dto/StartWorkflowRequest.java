package com.rishikanth.orchestrator.dto;

import com.rishikanth.orchestrator.model.ScenarioType;
import jakarta.validation.constraints.NotNull;

public class StartWorkflowRequest {
    @NotNull
    private ScenarioType scenario;

    @NotNull
    private String requirementText;

    /** Demo-only hook to exercise the rollback path deliberately. Defaults to false. */
    private boolean simulateReleaseFailure = false;

    /**
     * Explicit human authorization to let generated changes touch protected
     * core paths (domain layer, pom.xml). Defaults to false, matching the
     * principle that protected-path writes require opt-in, not opt-out.
     */
    private boolean changeControlApproved = false;

    public ScenarioType getScenario() { return scenario; }
    public void setScenario(ScenarioType scenario) { this.scenario = scenario; }
    public String getRequirementText() { return requirementText; }
    public void setRequirementText(String requirementText) { this.requirementText = requirementText; }
    public boolean isSimulateReleaseFailure() { return simulateReleaseFailure; }
    public void setSimulateReleaseFailure(boolean simulateReleaseFailure) { this.simulateReleaseFailure = simulateReleaseFailure; }
    public boolean isChangeControlApproved() { return changeControlApproved; }
    public void setChangeControlApproved(boolean changeControlApproved) { this.changeControlApproved = changeControlApproved; }
}
