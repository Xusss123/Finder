package karm.van.repo;

import karm.van.model.CardModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepo extends JpaRepository<CardModel,Long> {
    Optional<CardModel> getCardModelById(Long id);

    boolean existsById(Long id);
}
