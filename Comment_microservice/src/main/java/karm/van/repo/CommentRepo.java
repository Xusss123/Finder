package karm.van.repo;

import karm.van.model.CommentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepo extends JpaRepository<CommentModel,Long> {
    List<CommentModel> getCommentModelByCard_Id(Long id);

    Page<CommentModel> getCommentModelByCard_Id(Long id, Pageable pageable);
    Page<CommentModel> getCommentModelsByParentComment_Id(Long parentCommentId, Pageable pageable);


    void deleteAllByCard_Id(Long id);

    Optional<CommentModel> getCommentModelById(Long id);
}
