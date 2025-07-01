package dev.mars.generic.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.mars.dto.PagedResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generic response wrapper for API endpoints
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse {
    private String type; // SINGLE, PAGED, LIST
    private Object data;
    private Map<String, Object> metadata;
    private PaginationInfo pagination;
    private Long timestamp;

    // Default constructor
    public GenericResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for single object response
    public GenericResponse(Object data) {
        this();
        this.type = "SINGLE";
        this.data = data;
    }

    // Constructor for list response
    public GenericResponse(List<?> data) {
        this();
        this.type = "LIST";
        this.data = data;
    }

    // Constructor for paged response
    public GenericResponse(List<?> data, PaginationInfo pagination) {
        this();
        this.type = "PAGED";
        this.data = data;
        this.pagination = pagination;
    }

    // Static factory methods
    public static GenericResponse single(Object data) {
        return new GenericResponse(data);
    }

    public static GenericResponse list(List<?> data) {
        return new GenericResponse(data);
    }

    public static GenericResponse paged(List<?> data, int page, int size, long totalElements) {
        PaginationInfo pagination = new PaginationInfo(page, size, totalElements);
        return new GenericResponse(data, pagination);
    }

    public static GenericResponse fromPagedResponse(PagedResponse<?> pagedResponse) {
        PaginationInfo pagination = new PaginationInfo(
            pagedResponse.getPage(),
            pagedResponse.getSize(),
            pagedResponse.getTotalElements()
        );
        return new GenericResponse(pagedResponse.getData(), pagination);
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public PaginationInfo getPagination() {
        return pagination;
    }

    public void setPagination(PaginationInfo pagination) {
        this.pagination = pagination;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericResponse that = (GenericResponse) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(data, that.data) &&
               Objects.equals(metadata, that.metadata) &&
               Objects.equals(pagination, that.pagination) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data, metadata, pagination, timestamp);
    }

    @Override
    public String toString() {
        return "GenericResponse{" +
               "type='" + type + '\'' +
               ", data=" + data +
               ", metadata=" + metadata +
               ", pagination=" + pagination +
               ", timestamp=" + timestamp +
               '}';
    }

    /**
     * Pagination information for paged responses
     */
    public static class PaginationInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;

        // Default constructor
        public PaginationInfo() {}

        // Constructor with calculation
        public PaginationInfo(int page, int size, long totalElements) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / size);
            this.first = page == 0;
            this.last = page >= totalPages - 1;
        }

        // Getters and Setters
        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public boolean isFirst() {
            return first;
        }

        public void setFirst(boolean first) {
            this.first = first;
        }

        public boolean isLast() {
            return last;
        }

        public void setLast(boolean last) {
            this.last = last;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PaginationInfo that = (PaginationInfo) o;
            return page == that.page &&
                   size == that.size &&
                   totalElements == that.totalElements &&
                   totalPages == that.totalPages &&
                   first == that.first &&
                   last == that.last;
        }

        @Override
        public int hashCode() {
            return Objects.hash(page, size, totalElements, totalPages, first, last);
        }

        @Override
        public String toString() {
            return "PaginationInfo{" +
                   "page=" + page +
                   ", size=" + size +
                   ", totalElements=" + totalElements +
                   ", totalPages=" + totalPages +
                   ", first=" + first +
                   ", last=" + last +
                   '}';
        }
    }
}
