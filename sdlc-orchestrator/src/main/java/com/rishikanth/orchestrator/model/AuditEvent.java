package com.rishikanth.orchestrator.model;

import java.time.Instant;

/** One immutable, append-only audit record. Never mutated after creation. */
public class AuditEvent {
    private final Instant timestamp = Instant.now();
    private final String nodeId;
    private final NodeStatus from;
    private final NodeStatus to;
    private final Actor actor;
    private final String message;

    public AuditEvent(String nodeId, NodeStatus from, NodeStatus to, Actor actor, String message) {
        this.nodeId = nodeId;
        this.from = from;
        this.to = to;
        this.actor = actor;
        this.message = message;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getNodeId() { return nodeId; }
    public NodeStatus getFrom() { return from; }
    public NodeStatus getTo() { return to; }
    public Actor getActor() { return actor; }
    public String getMessage() { return message; }
}
