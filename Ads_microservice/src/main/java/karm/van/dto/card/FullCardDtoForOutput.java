package karm.van.dto.card;

import karm.van.dto.image.ImageDto;

import java.time.LocalDateTime;
import java.util.List;

public record FullCardDtoForOutput(Long id,
                                   String title,
                                   String text,
                                   LocalDateTime createTime,
                                   List<ImageDto> images,
                                   String authorName) {
}
