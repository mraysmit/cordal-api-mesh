package dev.cordal.dto;

import dev.cordal.generic.model.PagedResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PagedResponse
 */
class PagedResponseTest {

    @Test
    void testDefaultConstructor() {
        PagedResponse<String> response = new PagedResponse<>();
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNull();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(0);
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();
    }

    @Test
    void testParameterizedConstructor() {
        List<String> data = Arrays.asList("item1", "item2", "item3");
        PagedResponse<String> response = new PagedResponse<>(data, 0, 3, 10);

        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(3);
        assertThat(response.getTotalElements()).isEqualTo(10);
        assertThat(response.getTotalPages()).isEqualTo(4); // ceil(10/3) = 4
        assertThat(response.isHasNext()).isTrue(); // page 0 < totalPages-1 (3)
        assertThat(response.isHasPrevious()).isFalse(); // page 0
    }

    @Test
    void testStaticOfMethod() {
        List<String> data = Arrays.asList("item1", "item2");
        PagedResponse<String> response = PagedResponse.of(data, 1, 2, 5);

        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3); // ceil(5/2) = 3
        assertThat(response.isHasNext()).isTrue(); // page 1 < totalPages-1 (2)
        assertThat(response.isHasPrevious()).isTrue(); // page 1 > 0
    }

    @Test
    void testLastPage() {
        List<String> data = Arrays.asList("item1");
        PagedResponse<String> response = new PagedResponse<>(data, 2, 2, 5);

        assertThat(response.getTotalPages()).isEqualTo(3); // ceil(5/2) = 3
        assertThat(response.isHasNext()).isFalse(); // page 2 == totalPages-1 (2)
        assertThat(response.isHasPrevious()).isTrue(); // page 2 > 0
    }

    @Test
    void testSinglePage() {
        List<String> data = Arrays.asList("item1", "item2");
        PagedResponse<String> response = new PagedResponse<>(data, 0, 5, 2);

        assertThat(response.getTotalPages()).isEqualTo(1); // ceil(2/5) = 1
        assertThat(response.isHasNext()).isFalse(); // page 0 == totalPages-1 (0)
        assertThat(response.isHasPrevious()).isFalse(); // page 0
    }

    @Test
    void testEmptyData() {
        List<String> data = Arrays.asList();
        PagedResponse<String> response = new PagedResponse<>(data, 0, 10, 0);

        assertThat(response.getData()).isEmpty();
        assertThat(response.getTotalPages()).isEqualTo(0); // ceil(0/10) = 0
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();
    }

    @Test
    void testSettersAndGetters() {
        PagedResponse<String> response = new PagedResponse<>();
        List<String> data = Arrays.asList("test");

        response.setData(data);
        response.setPage(1);
        response.setSize(10);
        response.setTotalElements(25);
        response.setTotalPages(3);
        response.setHasNext(true);
        response.setHasPrevious(true);

        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isHasPrevious()).isTrue();
    }

    @Test
    void testTotalPagesCalculation() {
        // Test various scenarios for total pages calculation
        assertThat(new PagedResponse<>(Arrays.asList(), 0, 10, 0).getTotalPages()).isEqualTo(0);
        assertThat(new PagedResponse<>(Arrays.asList(), 0, 10, 1).getTotalPages()).isEqualTo(1);
        assertThat(new PagedResponse<>(Arrays.asList(), 0, 10, 10).getTotalPages()).isEqualTo(1);
        assertThat(new PagedResponse<>(Arrays.asList(), 0, 10, 11).getTotalPages()).isEqualTo(2);
        assertThat(new PagedResponse<>(Arrays.asList(), 0, 10, 20).getTotalPages()).isEqualTo(2);
        assertThat(new PagedResponse<>(Arrays.asList(), 0, 10, 21).getTotalPages()).isEqualTo(3);
    }
}
