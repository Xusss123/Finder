package karm.van.repository;

import karm.van.model.CardModel;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

public interface CardRepo extends JpaRepository<CardModel,Long> {
    Optional<CardModel> getCardModelById(Long id);

    List<CardModel> findAllByUserId(Long userId);
}
