package com.streametl.mapper;

import com.streametl.dto.ProcessResponse;
import com.streametl.dto.ViolationResponse;
import com.streametl.model.Record;
import com.streametl.violation.Violation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PipelineMapper {

    public ProcessResponse toProcessResponse(Record record) {
        return new ProcessResponse(
                record.getFields(),
                record.getMetadata().layer(),
                record.getMetadata().sourceId(),
                record.getMetadata().version());
    }

    public List<ViolationResponse> toViolationResponses(List<Violation> violations) {
        return violations.stream()
                .map(this::toViolationResponse)
                .toList();
    }

    private ViolationResponse toViolationResponse(Violation violation) {
        return new ViolationResponse(
                violation.ruleId(),
                violation.field(),
                violation.actualValue(),
                violation.severity(),
                violation.detectedAt());
    }
}
