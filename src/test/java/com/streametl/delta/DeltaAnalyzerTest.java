package com.streametl.delta;

import com.streametl.model.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeltaAnalyzerTest {

    private DeltaAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DeltaAnalyzer();
    }

    @Test
    void detectsModifiedField() {
        Record previous = Record.of(Map.of("id", "1", "price", 100), "src");
        Record current = Record.of(Map.of("id", "1", "price", 150), "src");

        List<DeltaResult> deltas = analyzer.compare(previous, current);

        assertThat(deltas).hasSize(1);
        assertThat(deltas.getFirst().changeType()).isEqualTo(ChangeType.MODIFIED);
        assertThat(deltas.getFirst().field()).isEqualTo("price");
        assertThat(deltas.getFirst().oldValue()).isEqualTo(100);
        assertThat(deltas.getFirst().newValue()).isEqualTo(150);
    }

    @Test
    void detectsAddedField() {
        Record previous = Record.of(Map.of("id", "1"), "src");
        Record current = Record.of(Map.of("id", "1", "name", "Alice"), "src");

        List<DeltaResult> deltas = analyzer.compare(previous, current);

        assertThat(deltas).hasSize(1);
        assertThat(deltas.getFirst().changeType()).isEqualTo(ChangeType.ADDED);
        assertThat(deltas.getFirst().field()).isEqualTo("name");
        assertThat(deltas.getFirst().oldValue()).isNull();
        assertThat(deltas.getFirst().newValue()).isEqualTo("Alice");
    }

    @Test
    void detectsDeletedField() {
        Record previous = Record.of(Map.of("id", "1", "status", "active"), "src");
        Record current = Record.of(Map.of("id", "1"), "src");

        List<DeltaResult> deltas = analyzer.compare(previous, current);

        assertThat(deltas).hasSize(1);
        assertThat(deltas.getFirst().changeType()).isEqualTo(ChangeType.DELETED);
        assertThat(deltas.getFirst().field()).isEqualTo("status");
        assertThat(deltas.getFirst().oldValue()).isEqualTo("active");
    }

    @Test
    void returnsEmptyDeltaForIdenticalRecords() {
        Record r = Record.of(Map.of("id", "1", "price", 100, "name", "Test"), "src");

        assertThat(analyzer.compare(r, r)).isEmpty();
        assertThat(analyzer.hasChanges(r, r)).isFalse();
    }

    @Test
    void detectsMultipleChangesSimultaneously() {
        Record previous = Record.of(Map.of("id", "1", "price", 100, "status", "active"), "src");
        Record current = Record.of(Map.of("id", "1", "price", 200, "category", "A"), "src");

        List<DeltaResult> deltas = analyzer.compare(previous, current);

        assertThat(deltas).hasSize(3);
        assertThat(deltas.stream().map(DeltaResult::changeType))
                .containsExactlyInAnyOrder(ChangeType.MODIFIED, ChangeType.DELETED, ChangeType.ADDED);
    }

    @Test
    void compareByChangeTypeFiltersCorrectly() {
        Record previous = Record.of(Map.of("id", "1", "price", 100, "status", "old"), "src");
        Record current = Record.of(Map.of("id", "1", "price", 200, "newField", "value"), "src");

        List<DeltaResult> added = analyzer.compareByChangeType(previous, current, ChangeType.ADDED);
        List<DeltaResult> deleted = analyzer.compareByChangeType(previous, current, ChangeType.DELETED);
        List<DeltaResult> modified = analyzer.compareByChangeType(previous, current, ChangeType.MODIFIED);

        assertThat(added).hasSize(1);
        assertThat(deleted).hasSize(1);
        assertThat(modified).hasSize(1);
    }

    @Test
    void compareSnapshotsDetectsNewRecord() {
        Record r1 = Record.of(Map.of("id", "1", "name", "Alice"), "src");
        Record r2 = Record.of(Map.of("id", "2", "name", "Bob"), "src");

        List<DeltaResult> deltas = analyzer.compareSnapshots(List.of(r1), List.of(r1, r2), "id");

        assertThat(deltas.stream()
                .filter(delta -> delta.changeType() == ChangeType.ADDED)
                .count()).isEqualTo(1);
    }

    @Test
    void compareSnapshotsDetectsRemovedRecord() {
        Record r1 = Record.of(Map.of("id", "1", "name", "Alice"), "src");
        Record r2 = Record.of(Map.of("id", "2", "name", "Bob"), "src");

        List<DeltaResult> deltas = analyzer.compareSnapshots(List.of(r1, r2), List.of(r1), "id");

        assertThat(deltas.stream()
                .filter(delta -> delta.changeType() == ChangeType.DELETED)
                .count()).isEqualTo(1);
    }

    @Test
    void hasChangesReturnsTrueWhenFieldsModified() {
        Record previous = Record.of(Map.of("price", 10), "src");
        Record current = Record.of(Map.of("price", 20), "src");

        assertThat(analyzer.hasChanges(previous, current)).isTrue();
    }

    @Test
    void compareSnapshotsThrowsOnDuplicateKey() {
        Record r1 = Record.of(Map.of("id", "1", "name", "Alice"), "src");
        Record r2 = Record.of(Map.of("id", "1", "name", "Duplicate"), "src");

        assertThatThrownBy(() -> analyzer.compareSnapshots(List.of(r1, r2), List.of(r1), "id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate key");
    }
}
