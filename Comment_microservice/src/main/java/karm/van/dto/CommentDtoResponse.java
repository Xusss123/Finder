package karm.van.dto;

import java.time.LocalDateTime;

public record CommentDtoResponse(String text, LocalDateTime createdAt, CommentAuthorDto commentAuthorDto) {
}
