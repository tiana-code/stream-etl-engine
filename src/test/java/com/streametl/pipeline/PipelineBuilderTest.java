package com.streametl.pipeline;

import com.streametl.model.Record;
import com.streametl.pipeline.impl.OutputStage;
import com.streametl.pipeline.impl.TransformationStage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineBuilderTest {

    @Test
    void buildWithSingleStageShouldSucceed() {
        EtlPipeline pipeline = PipelineBuilder.create()
                .validate("id")
                .build();

        assertThat(pipeline.stageCount()).isEqualTo(1);
    }

    @Test
    void buildWithNoStagesShouldThrow() {
        assertThatThrownBy(() -> PipelineBuilder.create().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one stage");
    }

    @Test
    void pipelineProcessesRecordThroughAllStages() {
        OutputStage output = OutputStage.capturing();

        EtlPipeline pipeline = PipelineBuilder.create()
                .validate("id", "name")
                .transform(TransformationStage.builder()
                        .rename("name", "fullName")
                        .trim("fullName")
                        .build())
                .stage(output)
                .build();

        Record input = Record.of(Map.of("id", "123", "name", "  Alice  "), "test");
        Record result = pipeline.process(input);

        assertThat(result.has("fullName")).isTrue();
        assertThat(result.get("fullName")).isEqualTo("Alice");
        assertThat(result.has("name")).isFalse();
        assertThat(output.getEmittedRecords()).hasSize(1);
    }

    @Test
    void processAllReturnsAllTransformedRecords() {
        EtlPipeline pipeline = PipelineBuilder.create()
                .validateSoft("id")
                .outputNoOp()
                .build();

        List<Record> records = List.of(
                Record.of(Map.of("id", "1", "value", 100), "src"),
                Record.of(Map.of("id", "2", "value", 200), "src"),
                Record.of(Map.of("id", "3", "value", 300), "src")
        );

        List<Record> results = pipeline.processAll(records);

        assertThat(results).hasSize(3);
    }

    @Test
    void validationStageThrowsOnMissingRequiredField() {
        EtlPipeline pipeline = PipelineBuilder.create()
                .validate("id", "amount")
                .build();

        Record incomplete = Record.of(Map.of("id", "123"), "test");

        assertThatThrownBy(() -> pipeline.process(incomplete))
                .isInstanceOf(PipelineValidationException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void softValidationDoesNotThrowOnMissingFields() {
        EtlPipeline pipeline = PipelineBuilder.create()
                .validateSoft("optional-field")
                .build();

        Record record = Record.of(Map.of("id", "1"), "test");
        Record result = pipeline.process(record);

        assertThat(result.get("_validationPassed")).isEqualTo(false);
    }

    @Test
    void enrichmentStageAddsFieldsFromReferenceData() {
        Map<Object, Map<String, Object>> referenceData = Map.of(
                "CAT-A", Map.of("category", "Electronics", "tax_rate", new BigDecimal("0.20"))
        );

        EtlPipeline pipeline = PipelineBuilder.create()
                .enrichFromMap("categoryCode", referenceData)
                .build();

        Record input = Record.of(Map.of("id", "1", "categoryCode", "CAT-A"), "test");
        Record result = pipeline.process(input);

        assertThat(result.get("category")).isEqualTo("Electronics");
        assertThat(result.get("tax_rate")).isEqualTo(new BigDecimal("0.20"));
    }

    @Test
    void describeReturnsNonEmptyString() {
        EtlPipeline pipeline = PipelineBuilder.create()
                .validate("id")
                .build();

        assertThat(pipeline.describe()).isNotBlank();
    }
}
