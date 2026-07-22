package com.rishikanth.orchestrator.controller;

import com.rishikanth.orchestrator.dto.*;
import com.rishikanth.orchestrator.engine.MetricsSnapshot;
import com.rishikanth.orchestrator.engine.WorkflowEngine;
import com.rishikanth.orchestrator.model.AuditEvent;
import com.rishikanth.orchestrator.model.WorkflowInstance;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    private final WorkflowEngine engine;

    public WorkflowController(WorkflowEngine engine) {
        this.engine = engine;
    }

    @PostMapping
    public ResponseEntity<WorkflowView> start(@Valid @RequestBody StartWorkflowRequest request) {
        WorkflowInstance instance = engine.startWorkflow(
                request.getScenario(), request.getRequirementText(),
                request.isSimulateReleaseFailure(), request.isChangeControlApproved());
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkflowView.of(instance));
    }

    @GetMapping
    public List<WorkflowView> listAll() {
        return engine.listAll().stream().map(WorkflowView::of).toList();
    }

    @GetMapping("/{id}")
    public WorkflowView get(@PathVariable String id) {
        return WorkflowView.of(engine.get(id));
    }

    @PostMapping("/{id}/nodes/{nodeId}/approve")
    public WorkflowView approve(@PathVariable String id, @PathVariable String nodeId,
                                 @Valid @RequestBody HumanActionRequest request) {
        return WorkflowView.of(engine.approve(id, nodeId, request.getNote()));
    }

    @PostMapping("/{id}/nodes/{nodeId}/clarify")
    public WorkflowView clarify(@PathVariable String id, @PathVariable String nodeId,
                                 @Valid @RequestBody HumanActionRequest request) {
        return WorkflowView.of(engine.clarify(id, nodeId, request.getNote()));
    }

    @GetMapping("/{id}/audit")
    public List<AuditEvent> audit(@PathVariable String id) {
        return engine.get(id).getAuditLog();
    }

    @GetMapping("/{id}/metrics")
    public MetricsSnapshot metrics(@PathVariable String id) {
        return engine.metrics(id);
    }
}
