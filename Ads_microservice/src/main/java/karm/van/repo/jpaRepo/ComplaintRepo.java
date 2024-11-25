package karm.van.repo.jpaRepo;

import karm.van.dto.complaint.ComplaintType;
import karm.van.model.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepo extends JpaRepository<Complaint,Long> {
    Page<Complaint> findAllByComplaintType(Pageable pageable, ComplaintType complaintType);
    void deleteAllByComplaintAuthorId(Long authorId);
    void deleteAllByTargetIdAndComplaintType(Long targetId, ComplaintType complaintType);
}
