package dev.mars.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PagedResponse
 */
class PagedResponseTest {

    @Test
    void shouldCreatePagedResponseWithData() {
        List<String> data = Arrays.asList("item1", "item2", "item3");
        PagedResponse<String> response = new PagedResponse<>(data, 0, 10, 25);

        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3); // ceil(25/10)
        assertThat(response.isHasNext()).isTrue(); // page 0 of 3 pages
        assertThat(response.isHasPrevious()).isFalse(); // first page
    }

    @Test
    void shouldCalculateCorrectPaginationForMiddlePage() {
        List<Integer> data = Arrays.asList(11, 12, 13, 14, 15);
        PagedResponse<Integer> response = new PagedResponse<>(data, 1, 5, 23);

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(23);
        assertThat(response.getTotalPages()).isEqualTo(5); // ceil(23/5)
        assertThat(response.isHasNext()).isTrue(); // page 1 of 5 pages
        assertThat(response.isHasPrevious()).isTrue(); // not first page
    }

    @Test
    void shouldCalculateCorrectPaginationForLastPage() {
        List<String> data = Arrays.asList("last1", "last2", "last3");
        PagedResponse<String> response = new PagedResponse<>(data, 2, 5, 13);

        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(13);
        assertThat(response.getTotalPages()).isEqualTo(3); // ceil(13/5)
        assertThat(response.isHasNext()).isFalse(); // last page
        assertThat(response.isHasPrevious()).isTrue(); // not first page
    }

    @Test
    void shouldHandleSinglePage() {
        List<String> data = Arrays.asList("only1", "only2");
        PagedResponse<String> response = new PagedResponse<>(data, 0, 10, 2);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();
    }

    @Test
    void shouldHandleEmptyData() {
        List<String> data = Arrays.asList();
        PagedResponse<String> response = new PagedResponse<>(data, 0, 10, 0);

        assertThat(response.getData()).isEmpty();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();
    }

    @Test
    void shouldCreateEmptyPagedResponse() {
        PagedResponse<String> response = new PagedResponse<>();

        assertThat(response.getData()).isNull();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(0);
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getTotalPages()).isEqualTo(0);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();
    }

    @Test
    void shouldSetAndGetAllProperties() {
        PagedResponse<String> response = new PagedResponse<>();
        
        List<String> data = Arrays.asList("test1", "test2");
        response.setData(data);
        response.setPage(1);
        response.setSize(5);
        response.setTotalElements(15);
        response.setTotalPages(3);
        response.setHasNext(true);
        response.setHasPrevious(true);

        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(15);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isHasPrevious()).isTrue();
    }

    @Test
    void shouldHaveToStringMethod() {
        List<String> data = Arrays.asList("item1", "item2");
        PagedResponse<String> response = new PagedResponse<>(data, 0, 5, 10);

        String toString = response.toString();
        assertThat(toString).contains("page=0");
        assertThat(toString).contains("size=5");
        assertThat(toString).contains("totalElements=10");
        assertThat(toString).contains("dataSize=2");
    }
}
