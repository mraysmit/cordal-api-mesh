package dev.mars.generic.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GenericResponse
 */
class GenericResponseTest {

    @Test
    void testDefaultConstructor() {
        // Act
        GenericResponse response = new GenericResponse();

        // Assert
        assertThat(response.getType()).isNull();
        assertThat(response.getData()).isNull();
        assertThat(response.getMetadata()).isNull();
        assertThat(response.getPagination()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    }

    @Test
    void testSingleObjectConstructor() {
        // Arrange
        Map<String, Object> data = Map.of("id", 1, "name", "test");

        // Act
        GenericResponse response = new GenericResponse(data);

        // Assert
        assertThat(response.getType()).isEqualTo("SINGLE");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPagination()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testListConstructor() {
        // Arrange
        List<Map<String, Object>> data = List.of(
            Map.of("id", 1, "name", "test1"),
            Map.of("id", 2, "name", "test2")
        );

        // Act
        GenericResponse response = new GenericResponse(data);

        // Assert
        assertThat(response.getType()).isEqualTo("LIST");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPagination()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testPagedConstructor() {
        // Arrange
        List<Map<String, Object>> data = List.of(
            Map.of("id", 1, "name", "test1"),
            Map.of("id", 2, "name", "test2")
        );
        GenericResponse.PaginationInfo pagination = new GenericResponse.PaginationInfo(0, 10, 25);

        // Act
        GenericResponse response = new GenericResponse(data, pagination);

        // Assert
        assertThat(response.getType()).isEqualTo("PAGED");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPagination()).isEqualTo(pagination);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testSingleStaticFactory() {
        // Arrange
        Map<String, Object> data = Map.of("id", 1, "name", "test");

        // Act
        GenericResponse response = GenericResponse.single(data);

        // Assert
        assertThat(response.getType()).isEqualTo("SINGLE");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPagination()).isNull();
    }

    @Test
    void testListStaticFactory() {
        // Arrange
        List<Map<String, Object>> data = List.of(
            Map.of("id", 1, "name", "test1"),
            Map.of("id", 2, "name", "test2")
        );

        // Act
        GenericResponse response = GenericResponse.list(data);

        // Assert
        assertThat(response.getType()).isEqualTo("LIST");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPagination()).isNull();
    }

    @Test
    void testPagedStaticFactory() {
        // Arrange
        List<Map<String, Object>> data = List.of(
            Map.of("id", 1, "name", "test1"),
            Map.of("id", 2, "name", "test2")
        );

        // Act
        GenericResponse response = GenericResponse.paged(data, 1, 10, 25);

        // Assert
        assertThat(response.getType()).isEqualTo("PAGED");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPagination()).isNotNull();
        assertThat(response.getPagination().getPage()).isEqualTo(1);
        assertThat(response.getPagination().getSize()).isEqualTo(10);
        assertThat(response.getPagination().getTotalElements()).isEqualTo(25);
        assertThat(response.getPagination().getTotalPages()).isEqualTo(3);
        assertThat(response.getPagination().isLast()).isFalse(); // page 1 of 3, not last
        assertThat(response.getPagination().isFirst()).isFalse(); // page 1, not first (0)
    }

    @Test
    void testFromPagedResponseStaticFactory() {
        // Arrange
        List<Map<String, Object>> data = List.of(
            Map.of("id", 1, "name", "test1"),
            Map.of("id", 2, "name", "test2")
        );
        PagedResponse<Map<String, Object>> pagedResponse = new PagedResponse<>(data, 0, 10, 25);

        // Act
        GenericResponse response = GenericResponse.fromPagedResponse(pagedResponse);

        // Assert
        assertThat(response.getType()).isEqualTo("PAGED");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPagination()).isNotNull();
        assertThat(response.getPagination().getPage()).isEqualTo(0);
        assertThat(response.getPagination().getSize()).isEqualTo(10);
        assertThat(response.getPagination().getTotalElements()).isEqualTo(25);
    }

    @Test
    void testSettersAndGetters() {
        // Arrange
        GenericResponse response = new GenericResponse();
        Map<String, Object> data = Map.of("id", 1);
        Map<String, Object> metadata = Map.of("source", "test");
        GenericResponse.PaginationInfo pagination = new GenericResponse.PaginationInfo(0, 10, 25);

        // Act
        response.setType("CUSTOM");
        response.setData(data);
        response.setMetadata(metadata);
        response.setPagination(pagination);
        response.setTimestamp(12345L);

        // Assert
        assertThat(response.getType()).isEqualTo("CUSTOM");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getMetadata()).isEqualTo(metadata);
        assertThat(response.getPagination()).isEqualTo(pagination);
        assertThat(response.getTimestamp()).isEqualTo(12345L);
    }

    @Test
    void testMetadata() {
        // Test setting metadata manually
        GenericResponse response = new GenericResponse();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 123);

        response.setMetadata(metadata);

        assertThat(response.getMetadata()).isNotNull();
        assertThat(response.getMetadata()).containsEntry("key1", "value1");
        assertThat(response.getMetadata()).containsEntry("key2", 123);
    }

    @Test
    void testPaginationInfoConstructor() {
        // Act
        GenericResponse.PaginationInfo pagination = new GenericResponse.PaginationInfo(2, 10, 35);

        // Assert
        assertThat(pagination.getPage()).isEqualTo(2);
        assertThat(pagination.getSize()).isEqualTo(10);
        assertThat(pagination.getTotalElements()).isEqualTo(35);
        assertThat(pagination.getTotalPages()).isEqualTo(4);
        assertThat(pagination.isLast()).isFalse(); // page 2 of 4, not last
        assertThat(pagination.isFirst()).isFalse(); // page 2, not first (0)
    }

}
