package karm.van.dto.complaint;

import karm.van.complaint.ComplaintType;

public record ComplaintDtoRequest(

        ComplaintType targetType,
        String reason,
        Long complaintTargetId
) {
}
