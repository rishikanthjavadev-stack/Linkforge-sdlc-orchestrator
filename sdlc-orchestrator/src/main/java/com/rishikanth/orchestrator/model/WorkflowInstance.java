package com.rishikanth.orchestrator.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorkflowInstance {
    private final String id;
    private final ScenarioType scenarioType;
    private final List<NodeDefinition> definitions;
    private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<>();
    private final List<AuditEvent> auditLog = new CopyOnWriteArrayList<>();
    private final WorkflowContext context = new WorkflowContext();
    private final Instant createdAt = Instant.now();
    private volatile Instant completedAt;
    private volatile WorkflowStatus status = WorkflowStatus.RUNNING;

    public WorkflowInstance(String id, ScenarioType scenarioType, List<NodeDefinition> definitions) {
        this.id = id;
        this.scenarioType = scenarioType;
        this.definitions = definitions;
        for (NodeDefinition def : definitions) {
            nodeStates.put(def.getId(), new NodeState(def.getId()));
        }
    }

    public String getId() { return id; }
    public ScenarioType getScenarioType() { return scenarioType; }
    public List<NodeDefinition> getDefinitions() { return definitions; }
    public Map<String, NodeState> getNodeStates() { return nodeStates; }
    public NodeDefinition getDefinition(String nodeId) {
        return definitions.stream().filter(d -> d.getId().equals(nodeId)).findFirst().orElse(null);
    }
    public List<AuditEvent> getAuditLog() { return auditLog; }
    public void addAudit(AuditEvent event) { auditLog.add(event); }
    public WorkflowContext getContext() { return context; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }
}
