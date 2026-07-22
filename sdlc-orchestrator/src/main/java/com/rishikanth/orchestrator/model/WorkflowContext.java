package com.rishikanth.orchestrator.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The "shared context" that flows through every stage. Two distinct kinds
 * of state live here:
 *  - data: free-form key/value working state tasks read/write (e.g. the
 *    clarified requirement text, generated file paths)
 *  - decisions: an ordered, append-only rationale trail (see DecisionRecord)
 *
 * This is what lets IMPLEMENTATION read what REQUIREMENTS/DESIGN decided
 * without re-deriving it, and what lets a human audit *why* the workflow
 * ended up where it did.
 */
public class WorkflowContext {
    private final Map<String, String> data = new ConcurrentHashMap<>();
    private final List<DecisionRecord> decisions = new CopyOnWriteArrayList<>();

    public void put(String key, String value) { data.put(key, value); }
    public String get(String key) { return data.get(key); }
    public Map<String, String> snapshot() { return Map.copyOf(data); }

    public void recordDecision(Stage stage, String decision, String rationale, Actor actor) {
        decisions.add(new DecisionRecord(stage, decision, rationale, actor));
    }

    public List<DecisionRecord> getDecisions() { return List.copyOf(decisions); }
}
