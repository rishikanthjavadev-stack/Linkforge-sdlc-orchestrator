package com.rishikanth.orchestrator.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeControlPolicyTest {

    @TempDir
    Path repoRoot;

    @Test
    void blocksUnauthorizedWriteToServiceLayer() {
        Path candidate = repoRoot.resolve("src/main/java/com/rishikanth/linkforge/service/ShortUrlService.java");

        assertThatThrownBy(() -> ChangeControlPolicy.enforce(repoRoot, candidate, false))
                .isInstanceOf(ChangeControlPolicy.ChangeControlViolationException.class)
                .hasMessageContaining("service");
    }

    @Test
    void blocksUnauthorizedWriteToPomXml() {
        Path candidate = repoRoot.resolve("pom.xml");

        assertThatThrownBy(() -> ChangeControlPolicy.enforce(repoRoot, candidate, false))
                .isInstanceOf(ChangeControlPolicy.ChangeControlViolationException.class);
    }

    @Test
    void allowsWriteToServiceLayerWhenExplicitlyAuthorized() {
        Path candidate = repoRoot.resolve("src/main/java/com/rishikanth/linkforge/service/ShortUrlService.java");

        Path result = ChangeControlPolicy.enforce(repoRoot, candidate, true);

        assertThat(result).isEqualTo(candidate);
    }

    @Test
    void allowsUnauthorizedWriteToNonProtectedPath() {
        // filter/ and exception/ are NOT protected - this is the normal
        // brownfield rate-limiter path and must succeed without any approval
        Path candidate = repoRoot.resolve("src/main/java/com/rishikanth/linkforge/filter/RateLimitFilter.java");

        Path result = ChangeControlPolicy.enforce(repoRoot, candidate, false);

        assertThat(result).isEqualTo(candidate);
    }

    @Test
    void allowsUnauthorizedWriteToDocsDirectory() {
        Path candidate = repoRoot.resolve("docs/generated/some-summary.md");

        Path result = ChangeControlPolicy.enforce(repoRoot, candidate, false);

        assertThat(result).isEqualTo(candidate);
    }
}
