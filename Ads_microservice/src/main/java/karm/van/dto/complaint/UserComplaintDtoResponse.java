package karm.van.dto.complaint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserComplaintDtoResponse extends AbstractComplaint {
    private String userName;
    public UserComplaintDtoResponse(String userName, String reason, String complaintAuthorName, Long complaintId) {
        super(complaintId, reason, complaintAuthorName);
        this.userName = userName;
    }
}
