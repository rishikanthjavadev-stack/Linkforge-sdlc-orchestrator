package com.rishikanth.orchestrator.model;

public enum WorkflowStatus {
    RUNNING,
    AWAITING_HUMAN,   // at least one node paused for approval/clarification
    COMPLETED,
    SAFE_STOPPED,     // a critical node exhausted retries; workflow halted deliberately
    FAILED
}
