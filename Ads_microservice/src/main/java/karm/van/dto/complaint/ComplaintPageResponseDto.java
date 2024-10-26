package karm.van.dto.complaint;

import java.util.List;

public record ComplaintPageResponseDto(List<AbstractComplaint> complaints,
                                       boolean last,
                                       int totalPages,
                                       long totalElements,
                                       boolean first,
                                       int numberOfElements) {
}
