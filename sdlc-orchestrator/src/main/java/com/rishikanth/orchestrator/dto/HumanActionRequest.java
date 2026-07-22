package com.rishikanth.orchestrator.dto;

import jakarta.validation.constraints.NotBlank;

/** Used for both /approve (note = approval justification) and /clarify (note = clarification text). */
public class HumanActionRequest {
    @NotBlank
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
