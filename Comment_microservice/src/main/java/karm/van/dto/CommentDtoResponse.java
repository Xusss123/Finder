package karm.van.dto;

import java.time.LocalDateTime;

public record CommentDtoResponse(Long commentId,
                                 String text,
                                 LocalDateTime createdAt,
                                 CommentAuthorDto commentAuthorDto,
                                 int replyQuantity) {
}
