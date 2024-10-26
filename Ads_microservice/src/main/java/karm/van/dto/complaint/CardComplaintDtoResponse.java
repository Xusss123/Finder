package karm.van.dto.complaint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardComplaintDtoResponse extends AbstractComplaint {
    private Long cardId;
    public CardComplaintDtoResponse(Long cardId, String reason, String complaintAuthorName, Long complaintId) {
        super(complaintId,reason,complaintAuthorName);
        this.cardId = cardId;
    }
}
