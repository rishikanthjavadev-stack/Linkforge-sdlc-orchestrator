package com.rishikanth.orchestrator.tasks;

import com.rishikanth.orchestrator.model.NodeDefinition;
import com.rishikanth.orchestrator.model.TaskResult;
import com.rishikanth.orchestrator.model.WorkflowInstance;

/**
 * One "agent" responsible for one stage. Implementations must never throw
 * for expected/handleable conditions - return TaskResult.failure/ambiguous
 * instead, so the engine's retry/fallback/gate logic can react deliberately
 * rather than catching a generic exception.
 */
public interface StageTask {
    TaskResult execute(WorkflowInstance instance, NodeDefinition definition);
}
