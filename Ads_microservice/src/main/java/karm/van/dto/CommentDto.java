package karm.van.dto;

import java.time.LocalDateTime;

public record CommentDto(Long id, String text, LocalDateTime createdAt) {
}
