package com.rishikanth.orchestrator.model;

/** Who caused a given state transition - core to audit/decision lineage. */
public enum Actor {
    SYSTEM,
    AGENT,
    HUMAN
}
