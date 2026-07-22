package com.rishikanth.orchestrator.engine;

import com.rishikanth.orchestrator.model.*;
import com.rishikanth.orchestrator.tasks.ReleaseTask;
import com.rishikanth.orchestrator.tasks.StageTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Drives one or more WorkflowInstances through their DAGs. Every public
 * mutating method (startWorkflow, approve, clarify) triggers advance(),
 * which is the single place that decides what runs next - this keeps the
 * "what happens after X" logic in one auditable spot instead of scattered
 * across callbacks.
 */
@Service
public class WorkflowEngine {

    private final Map<String, WorkflowInstance> instances = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final GraphFactory graphFactory;
    private final TaskRegistry taskRegistry;
    private final ReleaseTask releaseTask;
    private final long retryBackoffMs;

    public WorkflowEngine(GraphFactory graphFactory, TaskRegistry taskRegistry, ReleaseTask releaseTask,
                           @Value("${orchestrator.retry-backoff-ms}") long retryBackoffMs) {
        this.graphFactory = graphFactory;
        this.taskRegistry = taskRegistry;
        this.releaseTask = releaseTask;
        this.retryBackoffMs = retryBackoffMs;
    }

    public WorkflowInstance startWorkflow(ScenarioType scenario, String requirementText, boolean simulateReleaseFailure) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        WorkflowInstance instance = new WorkflowInstance(id, scenario, graphFactory.build(scenario));
        instance.getContext().put("requirementText", requirementText);
        if (simulateReleaseFailure) {
            instance.getContext().put("simulateReleaseFailure", "true");
        }
        instances.put(id, instance);
        advance(instance);
        return instance;
    }

    public WorkflowInstance get(String id) {
        WorkflowInstance instance = instances.get(id);
        if (instance == null) throw new NoSuchElementException("No workflow with id " + id);
        return instance;
    }

    public java.util.Collection<WorkflowInstance> listAll() {
        return instances.values();
    }

    public static class NoSuchElementException extends RuntimeException {
        public NoSuchElementException(String message) { super(message); }
    }

    public static class InvalidTransitionException extends RuntimeException {
        public InvalidTransitionException(String message) { super(message); }
    }

    // ---- human gates ----

    public WorkflowInstance approve(String workflowId, String nodeId, String note) {
        WorkflowInstance instance = get(workflowId);
        NodeState state = instance.getNodeStates().get(nodeId);
        if (state == null || state.getStatus() != NodeStatus.AWAITING_APPROVAL) {
            throw new InvalidTransitionException("Node " + nodeId + " is not awaiting approval " +
                    "(current status: " + (state == null ? "unknown" : state.getStatus()) + ")");
        }
        state.setApprovalNote(note);
        transition(instance, state, NodeStatus.APPROVED, Actor.HUMAN, "Approved by human: " + note);
        executor.submit(() -> {
            runNode(instance, instance.getDefinition(nodeId));
            advance(instance);
        });
        return instance;
    }

    public WorkflowInstance clarify(String workflowId, String nodeId, String clarificationText) {
        WorkflowInstance instance = get(workflowId);
        NodeState state = instance.getNodeStates().get(nodeId);
        if (state == null || state.getStatus() != NodeStatus.BLOCKED_CLARIFICATION) {
            throw new InvalidTransitionException("Node " + nodeId + " is not blocked on clarification " +
                    "(current status: " + (state == null ? "unknown" : state.getStatus()) + ")");
        }
        instance.getContext().put("requirementText", clarificationText);
        instance.getContext().put("clarified", "true");
        instance.getContext().recordDecision(Stage.REQUIREMENTS,
                "Human clarified ambiguous requirement to: " + clarificationText,
                "Re-planning trigger: requirements stage re-executes with clarified input; " +
                "downstream stages had not yet run, so no invalidation of completed work was needed.",
                Actor.HUMAN);
        transition(instance, state, NodeStatus.PENDING, Actor.HUMAN, "Clarification received, re-queued");
        instance.setStatus(WorkflowStatus.RUNNING);
        advance(instance);
        return instance;
    }

    // ---- core scheduling loop ----

    private void advance(WorkflowInstance instance) {
        synchronized (instance) {
            if (instance.getStatus() == WorkflowStatus.SAFE_STOPPED || instance.getStatus() == WorkflowStatus.FAILED) {
                return;
            }
            boolean anyPendingOrActive = false;
            boolean anyFailed = false;
            boolean allTerminal = true;

            for (NodeDefinition def : instance.getDefinitions()) {
                NodeState state = instance.getNodeStates().get(def.getId());
                switch (state.getStatus()) {
                    case PENDING -> {
                        if (depsSatisfied(instance, def)) {
                            if (def.isRequiresApproval()) {
                                transition(instance, state, NodeStatus.AWAITING_APPROVAL, Actor.SYSTEM,
                                        "High-impact action - awaiting human approval before execution");
                                instance.setStatus(WorkflowStatus.AWAITING_HUMAN);
                            } else {
                                transition(instance, state, NodeStatus.RUNNING, Actor.SYSTEM, "Dependencies satisfied, starting");
                                final NodeDefinition d = def;
                                executor.submit(() -> {
                                    runNode(instance, d);
                                    advance(instance);
                                });
                            }
                            anyPendingOrActive = true;
                            allTerminal = false;
                        } else {
                            allTerminal = false;
                        }
                    }
                    case RUNNING, RETRYING, AWAITING_APPROVAL, APPROVED, BLOCKED_CLARIFICATION -> {
                        anyPendingOrActive = true;
                        allTerminal = false;
                    }
                    case FAILED -> anyFailed = true;
                    case COMPLETED, SKIPPED, ROLLED_BACK -> { /* terminal, no-op */ }
                    case SAFE_STOPPED -> { /* handled at workflow level already */ }
                }
            }

            if (!anyPendingOrActive) {
                if (instance.getStatus() != WorkflowStatus.SAFE_STOPPED) {
                    if (anyFailed) {
                        instance.setStatus(WorkflowStatus.FAILED);
                    } else if (allTerminal) {
                        instance.setStatus(WorkflowStatus.COMPLETED);
                    }
                }
                if (instance.getCompletedAt() == null &&
                        (instance.getStatus() == WorkflowStatus.COMPLETED || instance.getStatus() == WorkflowStatus.FAILED)) {
                    instance.setCompletedAt(java.time.Instant.now());
                }
            } else if (instance.getStatus() != WorkflowStatus.AWAITING_HUMAN) {
                instance.setStatus(WorkflowStatus.RUNNING);
            }
        }
    }

    private boolean depsSatisfied(WorkflowInstance instance, NodeDefinition def) {
        for (String depId : def.getDependsOn()) {
            NodeStatus depStatus = instance.getNodeStates().get(depId).getStatus();
            if (depStatus != NodeStatus.COMPLETED && depStatus != NodeStatus.SKIPPED) {
                return false;
            }
        }
        return true;
    }

    // ---- node execution with bounded retries + fallback + rollback ----

    private void runNode(WorkflowInstance instance, NodeDefinition def) {
        NodeState state = instance.getNodeStates().get(def.getId());
        StageTask task = taskRegistry.get(def.getStage());

        if (state.getStartedAt() == null) {
            state.setStartedAt(java.time.Instant.now());
        }

        int totalAttempts = def.getMaxRetries() + 1;
        TaskResult lastResult = null;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            state.incrementAttempts();
            if (attempt > 1) {
                transition(instance, state, NodeStatus.RETRYING, Actor.SYSTEM,
                        "Retry attempt " + attempt + "/" + totalAttempts);
                try { Thread.sleep(retryBackoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                transition(instance, state, NodeStatus.RUNNING, Actor.AGENT, "Retrying");
            }

            TaskResult result;
            try {
                result = task.execute(instance, def);
            } catch (Exception e) {
                result = TaskResult.failure("Unhandled exception in task: " + e.getMessage());
            }
            lastResult = result;

            if (result.getOutcome() == TaskResult.Outcome.AMBIGUOUS) {
                state.setCompletedAt(java.time.Instant.now());
                transition(instance, state, NodeStatus.BLOCKED_CLARIFICATION, Actor.AGENT, result.getMessage());
                instance.setStatus(WorkflowStatus.AWAITING_HUMAN);
                return;
            }
            if (result.getOutcome() == TaskResult.Outcome.SUCCESS || result.getOutcome() == TaskResult.Outcome.FALLBACK_SUCCESS) {
                state.setOutput(result.getOutput() != null ? result.getOutput() : result.getMessage());
                state.setFallbackUsed(result.getOutcome() == TaskResult.Outcome.FALLBACK_SUCCESS);
                state.setCompletedAt(java.time.Instant.now());
                transition(instance, state, NodeStatus.COMPLETED, Actor.AGENT,
                        state.isFallbackUsed() ? "Completed via fallback: " + result.getMessage() : "Completed successfully");
                return;
            }
            // FAILURE: loop continues until totalAttempts exhausted
            state.setLastError(result.getMessage());
        }

        // retries exhausted
        state.setCompletedAt(java.time.Instant.now());
        if (def.isCritical()) {
            if (def.getStage() == Stage.RELEASE) {
                releaseTask.rollback(instance);
                transition(instance, state, NodeStatus.ROLLED_BACK, Actor.SYSTEM,
                        "Retries exhausted post-approval; rolled back. Last error: " +
                        (lastResult != null ? lastResult.getMessage() : "unknown"));
            } else {
                transition(instance, state, NodeStatus.FAILED, Actor.SYSTEM,
                        "Retries exhausted on critical node. Last error: " +
                        (lastResult != null ? lastResult.getMessage() : "unknown"));
            }
            instance.setStatus(WorkflowStatus.SAFE_STOPPED);
        } else {
            transition(instance, state, NodeStatus.FAILED, Actor.SYSTEM,
                    "Retries exhausted on non-critical node. Last error: " +
                    (lastResult != null ? lastResult.getMessage() : "unknown"));
        }
    }

    private void transition(WorkflowInstance instance, NodeState state, NodeStatus to, Actor actor, String message) {
        NodeStatus from = state.getStatus();
        state.setStatus(to);
        instance.addAudit(new AuditEvent(state.getNodeId(), from, to, actor, message));
    }

    // ---- metrics ----

    public MetricsSnapshot metrics(String workflowId) {
        WorkflowInstance instance = get(workflowId);
        MetricsSnapshot snapshot = new MetricsSnapshot();
        snapshot.totalNodes = instance.getDefinitions().size();

        long recoveryDurationSum = 0;
        int recoveryCount = 0;

        for (NodeState state : instance.getNodeStates().values()) {
            if (state.getStatus() == NodeStatus.COMPLETED) snapshot.completedNodes++;
            if (state.getStatus() == NodeStatus.FAILED) snapshot.failedNodes++;
            if (state.getStatus() == NodeStatus.ROLLED_BACK) snapshot.rolledBackNodes++;
            if (state.getAttempts() > 1) {
                snapshot.totalRetries += (state.getAttempts() - 1);
                recoveryDurationSum += state.durationMillis();
                recoveryCount++;
            }
        }
        snapshot.successRate = snapshot.totalNodes == 0 ? 0 : (double) snapshot.completedNodes / snapshot.totalNodes;
        snapshot.meanTimeToRecoveryMillis = recoveryCount == 0 ? 0 : (double) recoveryDurationSum / recoveryCount;

        java.time.Instant end = instance.getCompletedAt() != null ? instance.getCompletedAt() : java.time.Instant.now();
        snapshot.endToEndLatencyMillis = end.toEpochMilli() - instance.getCreatedAt().toEpochMilli();
        return snapshot;
    }
}
