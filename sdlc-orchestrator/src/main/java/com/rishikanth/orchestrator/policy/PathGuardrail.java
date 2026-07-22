package com.rishikanth.orchestrator.policy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Change-control guardrail: any file the orchestrator writes on an agent's
 * behalf must resolve to a path underneath the configured target repo root.
 * This is a real check (path canonicalization + prefix test), not a
 * decorative comment - it exists to stop a future task implementation
 * (or a compromised/careless one) from writing outside the sanctioned
 * project directory, e.g. via a "../../" traversal in a generated filename.
 */
public class PathGuardrail {

    public static class PolicyViolationException extends RuntimeException {
        public PolicyViolationException(String message) { super(message); }
    }

    /**
     * Resolves symlinks on the allowed root, then walks up from the candidate
     * to find its nearest EXISTING ancestor and resolves symlinks on that
     * ancestor too, before reattaching the not-yet-created remainder. This
     * matters cross-platform: on macOS, /tmp is a symlink to /private/tmp, so
     * naively comparing a real-pathed root against a non-real-pathed candidate
     * (or vice versa) produces false-positive policy violations for paths
     * that are actually fine. Both sides must be resolved the same way.
     */
    public static Path enforce(Path allowedRoot, Path candidate) {
        try {
            Path resolvedRoot = allowedRoot.toRealPath();
            Path candidateAbs = candidate.toAbsolutePath().normalize();

            Path existingAncestor = candidateAbs;
            java.util.List<String> pendingParts = new java.util.ArrayList<>();
            while (existingAncestor != null && !Files.exists(existingAncestor)) {
                if (existingAncestor.getFileName() != null) {
                    pendingParts.add(0, existingAncestor.getFileName().toString());
                }
                existingAncestor = existingAncestor.getParent();
            }
            if (existingAncestor == null) {
                throw new PolicyViolationException("Candidate path has no existing ancestor: " + candidateAbs);
            }

            Path resolvedCandidate = existingAncestor.toRealPath();
            for (String part : pendingParts) {
                resolvedCandidate = resolvedCandidate.resolve(part);
            }

            if (!resolvedCandidate.normalize().startsWith(resolvedRoot)) {
                throw new PolicyViolationException(
                        "Blocked write outside allowed repo root. root=" + resolvedRoot
                                + " candidate=" + resolvedCandidate);
            }
            return resolvedCandidate;
        } catch (IOException e) {
            throw new PolicyViolationException("Could not resolve path for guardrail check: " + e.getMessage());
        }
    }
}
