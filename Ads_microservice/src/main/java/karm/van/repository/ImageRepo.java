package karm.van.repository;

import karm.van.model.CardModel;
import karm.van.model.ImageModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageRepo extends JpaRepository<ImageModel,Long> {
    int countAllByCard(CardModel cardModel);
}
