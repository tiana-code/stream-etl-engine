package com.streametl.datalake;

import com.streametl.model.DataLakeLayer;
import com.streametl.model.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LayerProcessorTest {

    private LayerProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new LayerProcessor();
    }

    @Test
    void toBronzeAssignsBronzeLayer() {
        Record record = Record.of(Map.of("id", "1", "raw", "data"), "source");

        Record bronze = processor.toBronze(record);

        assertThat(bronze.getMetadata().layer()).isEqualTo(DataLakeLayer.BRONZE);
    }

    @Test
    void toSilverRemovesNullFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "1");
        fields.put("name", null);
        fields.put("value", 100);

        Record record = Record.of(fields, "source");
        Record silver = processor.toSilver(record);

        assertThat(silver.getMetadata().layer()).isEqualTo(DataLakeLayer.SILVER);
        assertThat(silver.has("name")).isFalse();
        assertThat(silver.has("id")).isTrue();
        assertThat(silver.has("value")).isTrue();
    }

    @Test
    void toSilverRemovesBlankStringFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", "1");
        fields.put("description", "   ");
        fields.put("code", "ABC");

        Record record = Record.of(fields, "source");
        Record silver = processor.toSilver(record);

        assertThat(silver.has("description")).isFalse();
        assertThat(silver.has("code")).isTrue();
    }

    @Test
    void toGoldPromotesFromSilver() {
        Record record = Record.of(Map.of("id", "1", "value", 42), "source");
        Record silver = processor.toSilver(record);
        Record gold = processor.toGold(silver);

        assertThat(gold.getMetadata().layer()).isEqualTo(DataLakeLayer.GOLD);
    }

    @Test
    void toGoldThrowsWhenNotSilver() {
        Record bronze = processor.toBronze(Record.of(Map.of("id", "1"), "source"));

        assertThatThrownBy(() -> processor.toGold(bronze))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SILVER");
    }

    @Test
    void promoteAllConvertsAllRecordsToGold() {
        List<Record> bronzeRecords = List.of(
                Record.of(Map.of("id", "1", "val", 10), "src"),
                Record.of(Map.of("id", "2", "val", 20), "src"),
                Record.of(Map.of("id", "3", "val", 30), "src")
        );

        List<Record> gold = processor.promoteAll(bronzeRecords);

        assertThat(gold).hasSize(3);
        assertThat(gold).allMatch(record -> record.getMetadata().layer() == DataLakeLayer.GOLD);
    }

    @Test
    void aggregateGroupsByField() {
        List<Record> records = List.of(
                Record.of(Map.of("category", "A", "amount", 100), "src"),
                Record.of(Map.of("category", "B", "amount", 200), "src"),
                Record.of(Map.of("category", "A", "amount", 300), "src")
        );

        List<Record> gold = processor.promoteAll(records);
        LayerProcessor.AggregatedResult result = processor.aggregate(gold, "category");

        assertThat(result.totalRecords()).isEqualTo(3);
        assertThat(result.countsByGroup()).containsKey("A");
        assertThat(result.countsByGroup()).containsKey("B");
        assertThat(result.countsByGroup().get("A")).isEqualTo(2L);
        assertThat(result.countsByGroup().get("B")).isEqualTo(1L);
    }

    @Test
    void aggregateReturnsEmptyResultForNoRecords() {
        LayerProcessor.AggregatedResult result = processor.aggregate(List.of(), "category");

        assertThat(result.totalRecords()).isEqualTo(0);
        assertThat(result.countsByGroup()).isEmpty();
    }

    @Test
    void metadataVersionIncrementsOnLayerTransition() {
        Record raw = Record.of(Map.of("id", "1"), "source");
        int initialVersion = raw.getMetadata().version();

        Record silver = processor.toSilver(raw);

        assertThat(silver.getMetadata().version()).isGreaterThan(initialVersion);
    }

    @Test
    void dataLakeLayerCanPromoteToReturnsCorrectly() {
        assertThat(DataLakeLayer.BRONZE.canPromoteTo(DataLakeLayer.SILVER)).isTrue();
        assertThat(DataLakeLayer.SILVER.canPromoteTo(DataLakeLayer.GOLD)).isTrue();
        assertThat(DataLakeLayer.BRONZE.canPromoteTo(DataLakeLayer.GOLD)).isFalse();
        assertThat(DataLakeLayer.GOLD.canPromoteTo(DataLakeLayer.SILVER)).isFalse();
    }

    @Test
    void dataLakeLayerFromParsesUppercase() {
        assertThat(DataLakeLayer.from("GOLD")).isEqualTo(DataLakeLayer.GOLD);
    }

    @Test
    void dataLakeLayerFromParsesLowercase() {
        assertThat(DataLakeLayer.from("bronze")).isEqualTo(DataLakeLayer.BRONZE);
    }

    @Test
    void dataLakeLayerFromParsesMixedCase() {
        assertThat(DataLakeLayer.from("Silver")).isEqualTo(DataLakeLayer.SILVER);
    }

    @Test
    void dataLakeLayerFromThrowsOnInvalidValue() {
        assertThatThrownBy(() -> DataLakeLayer.from("PLATINUM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PLATINUM");
    }
}
