package com.streametl.controller;

import com.streametl.dto.HealthResponse;
import com.streametl.dto.PipelinePayloadRequest;
import com.streametl.dto.ProcessResponse;
import com.streametl.dto.ViolationResponse;
import com.streametl.mapper.PipelineMapper;
import com.streametl.service.PipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;
    private final PipelineMapper pipelineMapper;

    @PostMapping("/process")
    public ResponseEntity<ProcessResponse> process(
            @Valid @RequestBody PipelinePayloadRequest request,
            @RequestParam(defaultValue = "api") String sourceId) {
        return ResponseEntity.ok(
                pipelineMapper.toProcessResponse(pipelineService.process(request.payload(), sourceId)));
    }

    @PostMapping("/detect-violations")
    public ResponseEntity<List<ViolationResponse>> detectViolations(
            @Valid @RequestBody PipelinePayloadRequest request,
            @RequestParam(defaultValue = "api") String sourceId) {
        return ResponseEntity.ok(
                pipelineMapper.toViolationResponses(pipelineService.detectViolations(request.payload(), sourceId)));
    }

    @PostMapping("/promote")
    public ResponseEntity<ProcessResponse> promote(
            @Valid @RequestBody PipelinePayloadRequest request,
            @RequestParam(defaultValue = "api") String sourceId,
            @RequestParam(defaultValue = "GOLD") String targetLayer) {
        return ResponseEntity.ok(
                pipelineMapper.toProcessResponse(pipelineService.promote(request.payload(), sourceId, targetLayer)));
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", "stream-etl-engine"));
    }
}
