package com.rishikanth.orchestrator.model;

/**
 * The SDLC lifecycle stages this orchestrator coordinates. Each stage maps
 * to one or more graph nodes depending on scenario (see GraphFactory).
 */
public enum Stage {
    REQUIREMENTS,
    DESIGN,
    IMPLEMENTATION,
    TESTING,
    DOCUMENTATION,
    RELEASE
}
