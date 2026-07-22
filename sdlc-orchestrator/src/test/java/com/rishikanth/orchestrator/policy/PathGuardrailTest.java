package com.rishikanth.orchestrator.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathGuardrailTest {

    @TempDir
    Path root;

    @Test
    void allowsWriteInsideAllowedRoot() throws IOException {
        Path sub = root.resolve("src/main");
        Files.createDirectories(sub);
        Path candidate = sub.resolve("Foo.java");
        Path result = PathGuardrail.enforce(root, candidate);
        assertThat(result.startsWith(root.toRealPath())).isTrue();
    }

    @Test
    void blocksTraversalOutsideAllowedRoot() throws IOException {
        Path outsideDir = Files.createTempDirectory("outside");
        Path traversal = root.resolve("../" + outsideDir.getFileName() + "/evil.java");

        assertThatThrownBy(() -> PathGuardrail.enforce(root, traversal))
                .isInstanceOf(PathGuardrail.PolicyViolationException.class);
    }
}
