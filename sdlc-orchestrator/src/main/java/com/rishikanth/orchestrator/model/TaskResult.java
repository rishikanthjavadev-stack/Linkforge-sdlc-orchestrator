package com.rishikanth.orchestrator.model;

/**
 * What every stage task hands back to the engine. Deliberately does not
 * throw for expected/handleable conditions (e.g. ambiguity, external
 * dependency unavailable) - those are modeled as explicit result states
 * so the engine can apply policy (block vs fallback vs fail) rather than
 * reacting to a raw exception.
 */
public class TaskResult {
    public enum Outcome { SUCCESS, FAILURE, AMBIGUOUS, FALLBACK_SUCCESS }

    private final Outcome outcome;
    private final String output;
    private final String message;

    private TaskResult(Outcome outcome, String output, String message) {
        this.outcome = outcome;
        this.output = output;
        this.message = message;
    }

    public static TaskResult success(String output) {
        return new TaskResult(Outcome.SUCCESS, output, null);
    }

    public static TaskResult fallbackSuccess(String output, String reason) {
        return new TaskResult(Outcome.FALLBACK_SUCCESS, output, reason);
    }

    public static TaskResult failure(String message) {
        return new TaskResult(Outcome.FAILURE, null, message);
    }

    public static TaskResult ambiguous(String message) {
        return new TaskResult(Outcome.AMBIGUOUS, null, message);
    }

    public Outcome getOutcome() { return outcome; }
    public String getOutput() { return output; }
    public String getMessage() { return message; }
}
