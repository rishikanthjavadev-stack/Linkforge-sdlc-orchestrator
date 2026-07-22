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

    public ScenarioType getScenario() { return scenario; }
    public void setScenario(ScenarioType scenario) { this.scenario = scenario; }
    public String getRequirementText() { return requirementText; }
    public void setRequirementText(String requirementText) { this.requirementText = requirementText; }
    public boolean isSimulateReleaseFailure() { return simulateReleaseFailure; }
    public void setSimulateReleaseFailure(boolean simulateReleaseFailure) { this.simulateReleaseFailure = simulateReleaseFailure; }
}
