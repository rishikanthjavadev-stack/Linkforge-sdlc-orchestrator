package com.rishikanth.orchestrator.dto;

import com.rishikanth.orchestrator.model.NodeDefinition;
import com.rishikanth.orchestrator.model.NodeState;

public class NodeView {
    public String id;
    public String stage;
    public String status;
    public int attempts;
    public boolean requiresApproval;
    public boolean fallbackUsed;
    public String output;
    public String lastError;
    public java.util.Set<String> dependsOn;

    public static NodeView of(NodeDefinition def, NodeState state) {
        NodeView v = new NodeView();
        v.id = def.getId();
        v.stage = def.getStage().name();
        v.status = state.getStatus().name();
        v.attempts = state.getAttempts();
        v.requiresApproval = def.isRequiresApproval();
        v.fallbackUsed = state.isFallbackUsed();
        v.output = state.getOutput();
        v.lastError = state.getLastError();
        v.dependsOn = def.getDependsOn();
        return v;
    }
}
