package com.rishikanth.orchestrator.engine;

import com.rishikanth.orchestrator.model.NodeDefinition;
import com.rishikanth.orchestrator.model.ScenarioType;
import com.rishikanth.orchestrator.model.Stage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * All three scenarios share the same graph shape:
 *
 *   requirements -> design -> implementation -> [testing, documentation] -> release
 *                                                  (parallel, joined at release)
 *
 * The shape itself is deliberately identical across scenarios: what differs
 * is task BEHAVIOR (see StageTask implementations), not the graph topology.
 * This is what "non-linear, stateful execution... rather than simple linear
 * task chaining" means in practice - testing and documentation are
 * independent branches that both gate release (a synchronization join).
 */
@Component
public class GraphFactory {

    public List<NodeDefinition> build(ScenarioType scenario) {
        return List.of(
                new NodeDefinition("requirements", Stage.REQUIREMENTS, Set.of(),
                        false, true, 0),
                new NodeDefinition("design", Stage.DESIGN, Set.of("requirements"),
                        false, true, 1),
                new NodeDefinition("implementation", Stage.IMPLEMENTATION, Set.of("design"),
                        false, true, 1),
                new NodeDefinition("testing", Stage.TESTING, Set.of("implementation"),
                        false, false, 2),
                new NodeDefinition("documentation", Stage.DOCUMENTATION, Set.of("implementation"),
                        false, false, 1),
                new NodeDefinition("release", Stage.RELEASE, Set.of("testing", "documentation"),
                        true, true, 1)
        );
    }
}
