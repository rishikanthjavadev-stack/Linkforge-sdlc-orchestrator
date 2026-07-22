package com.rishikanth.orchestrator.model;

import java.time.Instant;

/**
 * Mutable runtime state for one node of one workflow instance.
 * Every field here is what audit/metrics reporting reads from.
 */
public class NodeState {
    private final String nodeId;
    private volatile NodeStatus status = NodeStatus.PENDING;
    private volatile int attempts = 0;
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private volatile String output;
    private volatile String lastError;
    private volatile boolean fallbackUsed = false;
    private volatile String approvalNote;

    public NodeState(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() { return nodeId; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void incrementAttempts() { this.attempts++; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
    public String getApprovalNote() { return approvalNote; }
    public void setApprovalNote(String approvalNote) { this.approvalNote = approvalNote; }

    public long durationMillis() {
        if (startedAt == null) return 0;
        Instant end = completedAt != null ? completedAt : Instant.now();
        return end.toEpochMilli() - startedAt.toEpochMilli();
    }
}
