package karm.van.dto.complaint;

public record ComplaintDtoRequest(

        ComplaintType targetType,
        String reason,
        Long complaintTargetId
) {
}
