package com.rishikanth.orchestrator.engine;

import com.rishikanth.orchestrator.model.NodeDefinition;
import com.rishikanth.orchestrator.model.ScenarioType;
import com.rishikanth.orchestrator.model.Stage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GraphFactoryTest {

    private final GraphFactory factory = new GraphFactory();

    @Test
    void testingAndDocumentationBothDependOnlyOnImplementation_enablingParallelExecution() {
        List<NodeDefinition> defs = factory.build(ScenarioType.GREENFIELD);
        NodeDefinition testing = find(defs, "testing");
        NodeDefinition documentation = find(defs, "documentation");

        assertThat(testing.getDependsOn()).isEqualTo(Set.of("implementation"));
        assertThat(documentation.getDependsOn()).isEqualTo(Set.of("implementation"));
    }

    @Test
    void releaseJoinsBothParallelBranches() {
        List<NodeDefinition> defs = factory.build(ScenarioType.GREENFIELD);
        NodeDefinition release = find(defs, "release");
        assertThat(release.getDependsOn()).isEqualTo(Set.of("testing", "documentation"));
        assertThat(release.isRequiresApproval()).isTrue();
        assertThat(release.isCritical()).isTrue();
    }

    @Test
    void requirementsHasNoDependencies_isEntryPoint() {
        List<NodeDefinition> defs = factory.build(ScenarioType.AMBIGUOUS);
        NodeDefinition requirements = find(defs, "requirements");
        assertThat(requirements.getDependsOn()).isEmpty();
    }

    @Test
    void allSixSdlcStagesArePresentExactlyOnce() {
        List<NodeDefinition> defs = factory.build(ScenarioType.BROWNFIELD);
        assertThat(defs).extracting(NodeDefinition::getStage)
                .containsExactlyInAnyOrder(Stage.REQUIREMENTS, Stage.DESIGN, Stage.IMPLEMENTATION,
                        Stage.TESTING, Stage.DOCUMENTATION, Stage.RELEASE);
    }

    private NodeDefinition find(List<NodeDefinition> defs, String id) {
        return defs.stream().filter(d -> d.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Node not found: " + id));
    }
}
