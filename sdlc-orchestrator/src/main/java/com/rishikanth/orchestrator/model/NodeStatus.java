package com.rishikanth.orchestrator.model;

/**
 * State machine for a single graph node. Transitions are enforced by
 * WorkflowEngine, never set directly by task code.
 *
 *   PENDING -> RUNNING -> COMPLETED
 *                      -> FAILED -> RETRYING -> RUNNING (bounded by maxRetries)
 *                      -> FAILED -> SAFE_STOPPED (retries exhausted, critical node)
 *   PENDING -> AWAITING_APPROVAL -> APPROVED -> RUNNING -> ...      (human gate)
 *   PENDING -> BLOCKED_CLARIFICATION -> COMPLETED                  (ambiguity gate)
 *   COMPLETED -> ROLLED_BACK                                       (post-hoc rollback)
 *   * -> SKIPPED                                                   (not applicable to scenario)
 */
public enum NodeStatus {
    PENDING,
    RUNNING,
    AWAITING_APPROVAL,
    APPROVED,
    BLOCKED_CLARIFICATION,
    RETRYING,
    COMPLETED,
    FAILED,
    ROLLED_BACK,
    SAFE_STOPPED,
    SKIPPED
}
