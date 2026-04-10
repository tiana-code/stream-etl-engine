package com.streametl.service;

import com.streametl.config.EtlProperties;
import com.streametl.datalake.LayerProcessor;
import com.streametl.model.DataLakeLayer;
import com.streametl.model.Record;
import com.streametl.pipeline.EtlPipeline;
import com.streametl.pipeline.PipelineBuilder;
import com.streametl.violation.Operator;
import com.streametl.violation.Violation;
import com.streametl.violation.ViolationDetector;
import com.streametl.violation.ViolationRule;
import com.streametl.violation.ViolationSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PipelineService {

    private final ViolationDetector violationDetector;
    private final LayerProcessor layerProcessor;
    private final List<ViolationRule> defaultRules;
    private final EtlPipeline defaultPipeline;

    public PipelineService(ViolationDetector violationDetector,
                           LayerProcessor layerProcessor,
                           EtlProperties properties) {
        this.violationDetector = violationDetector;
        this.layerProcessor = layerProcessor;
        this.defaultRules = buildDefaultRules(
                ViolationSeverity.from(properties.violation().defaultSeverity()));
        this.defaultPipeline = PipelineBuilder.create()
                .validate("id")
                .outputNoOp()
                .build();
    }

    public Record process(Map<String, Object> payload, String sourceId) {
        log.info("Processing record. sourceId={}", sourceId);
        return defaultPipeline.process(Record.of(payload, sourceId));
    }

    public List<Violation> detectViolations(Map<String, Object> payload, String sourceId) {
        log.info("Detecting violations. sourceId={}", sourceId);
        Record record = Record.of(payload, sourceId);
        return violationDetector.detect(record, defaultRules);
    }

    public Record promote(Map<String, Object> payload, String sourceId, String targetLayer) {
        DataLakeLayer layer = DataLakeLayer.from(targetLayer);
        log.info("Promoting record. sourceId={}, targetLayer={}", sourceId, layer);
        Record record = Record.of(payload, sourceId);

        return switch (layer) {
            case BRONZE -> record;
            case SILVER -> layerProcessor.toSilver(record);
            case GOLD -> layerProcessor.toGold(layerProcessor.toSilver(record));
        };
    }

    private static List<ViolationRule> buildDefaultRules(ViolationSeverity severity) {
        return List.of(
                new ViolationRule("qty-negative", "quantity",
                        Operator.LESS_THAN, BigDecimal.ZERO, severity),
                new ViolationRule("price-negative", "price",
                        Operator.LESS_THAN, BigDecimal.ZERO, severity)
        );
    }
}
