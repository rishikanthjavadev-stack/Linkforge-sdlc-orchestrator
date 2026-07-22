package com.rishikanth.orchestrator.engine;

import com.rishikanth.orchestrator.model.Stage;
import com.rishikanth.orchestrator.tasks.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskRegistry {

    private final Map<Stage, StageTask> tasks;

    public TaskRegistry(RequirementsTask requirementsTask, DesignTask designTask,
                         ImplementationTask implementationTask, TestingTask testingTask,
                         DocumentationTask documentationTask, ReleaseTask releaseTask) {
        this.tasks = Map.of(
                Stage.REQUIREMENTS, requirementsTask,
                Stage.DESIGN, designTask,
                Stage.IMPLEMENTATION, implementationTask,
                Stage.TESTING, testingTask,
                Stage.DOCUMENTATION, documentationTask,
                Stage.RELEASE, releaseTask
        );
    }

    public StageTask get(Stage stage) {
        StageTask task = tasks.get(stage);
        if (task == null) {
            throw new IllegalStateException("No task registered for stage: " + stage);
        }
        return task;
    }
}
