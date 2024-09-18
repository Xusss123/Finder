package karm.van.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FullCardDtoForOutput(Long id, String title, String text, LocalDateTime createTime, List<ImageDto> images) {
}
