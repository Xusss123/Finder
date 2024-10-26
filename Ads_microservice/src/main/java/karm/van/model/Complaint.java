package karm.van.model;

import jakarta.persistence.*;
import karm.van.complaint.ComplaintType;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ComplaintType complaintType;

    private Long targetId;
    private String reason;
    private Long complaintAuthorId;
}
