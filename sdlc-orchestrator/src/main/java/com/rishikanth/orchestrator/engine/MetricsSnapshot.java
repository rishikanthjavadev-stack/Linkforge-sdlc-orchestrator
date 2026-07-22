package com.rishikanth.orchestrator.engine;

public class MetricsSnapshot {
    public int totalNodes;
    public int completedNodes;
    public int failedNodes;
    public int rolledBackNodes;
    public double successRate;
    public int totalRetries;
    public long endToEndLatencyMillis;
    public double meanTimeToRecoveryMillis; // avg duration of nodes that needed >1 attempt
}
