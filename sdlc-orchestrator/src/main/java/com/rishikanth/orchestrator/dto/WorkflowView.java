package com.rishikanth.orchestrator.dto;

import com.rishikanth.orchestrator.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowView {
    public String id;
    public String scenario;
    public String status;
    public String createdAt;
    public String completedAt;
    public List<NodeView> nodes = new ArrayList<>();
    public Map<String, String> context;
    public List<String> decisions = new ArrayList<>();

    public static WorkflowView of(WorkflowInstance instance) {
        WorkflowView v = new WorkflowView();
        v.id = instance.getId();
        v.scenario = instance.getScenarioType().name();
        v.status = instance.getStatus().name();
        v.createdAt = instance.getCreatedAt().toString();
        v.completedAt = instance.getCompletedAt() != null ? instance.getCompletedAt().toString() : null;
        for (NodeDefinition def : instance.getDefinitions()) {
            v.nodes.add(NodeView.of(def, instance.getNodeStates().get(def.getId())));
        }
        v.context = instance.getContext().snapshot();
        for (DecisionRecord d : instance.getContext().getDecisions()) {
            v.decisions.add("[" + d.getStage() + " / " + d.getDecidedBy() + "] " + d.getDecision() +
                    " -- rationale: " + d.getRationale());
        }
        return v;
    }
}
