package com.rishikanth.orchestrator.policy;

import java.nio.file.Path;
import java.util.List;

/**
 * Change-control guardrail, distinct in purpose from PathGuardrail:
 *
 *  - PathGuardrail answers "is this write physically confined to the
 *    sanctioned repo?" (a security/containment concern).
 *  - ChangeControlPolicy answers "even inside the sanctioned repo, is this
 *    a file an autonomous agent should be allowed to touch without extra
 *    sign-off?" (a change-management concern).
 *
 * Protected paths represent the domain/business-logic core and build
 * configuration - the parts of a codebase where an unreviewed agent
 * change carries outsized risk (breaking an existing contract, silently
 * altering build behavior). Any task that would write to one of these
 * paths must have explicit authorization recorded in the workflow
 * context; otherwise the write is refused before it happens, not
 * reviewed after the fact.
 */
public class ChangeControlPolicy {

    public static class ChangeControlViolationException extends RuntimeException {
        public ChangeControlViolationException(String message) { super(message); }
    }

    private static final List<String> PROTECTED_RELATIVE_PATHS = List.of(
            "pom.xml",
            "src/main/java/com/rishikanth/linkforge/model",
            "src/main/java/com/rishikanth/linkforge/service",
            "src/main/java/com/rishikanth/linkforge/repository"
    );

    /**
     * @param repoRoot   absolute path to the repository root
     * @param candidate  absolute path the caller intends to write to
     * @param authorized whether a human has explicitly approved touching
     *                   protected paths for this workflow run (read from
     *                   WorkflowContext, e.g. "changeControlApproved")
     * @return the candidate path, unchanged, if the write is allowed
     * @throws ChangeControlViolationException if the candidate falls under
     *         a protected path and authorization was not granted
     */
    public static Path enforce(Path repoRoot, Path candidate, boolean authorized) {
        Path relative;
        try {
            relative = repoRoot.toAbsolutePath().normalize()
                    .relativize(candidate.toAbsolutePath().normalize());
        } catch (IllegalArgumentException e) {
            // candidate isn't even under repoRoot - PathGuardrail's concern, not this one
            return candidate;
        }

        String relativeStr = relative.toString().replace('\\', '/');

        boolean touchesProtected = PROTECTED_RELATIVE_PATHS.stream()
                .anyMatch(protectedPath -> relativeStr.equals(protectedPath)
                        || relativeStr.startsWith(protectedPath + "/"));

        if (touchesProtected && !authorized) {
            throw new ChangeControlViolationException(
                    "Change-control policy blocked write to protected path '" + relativeStr +
                    "' - this is domain/build-core code. An agent may not modify it without " +
                    "explicit human authorization (set changeControlApproved=true for this workflow).");
        }

        return candidate;
    }
}
