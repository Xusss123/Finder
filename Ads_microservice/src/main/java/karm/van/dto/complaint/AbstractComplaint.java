package karm.van.dto.complaint;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserComplaintDtoResponse.class, name = "user"),
        @JsonSubTypes.Type(value = CardComplaintDtoResponse.class, name = "card")
})
@Getter
@Setter
@AllArgsConstructor
public abstract class AbstractComplaint {
    private Long complaintId;
    private String reason;
    private String complaintAuthorName;
}
