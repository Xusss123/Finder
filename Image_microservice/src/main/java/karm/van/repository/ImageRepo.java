package karm.van.repository;

import karm.van.model.ImageModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepo extends JpaRepository<ImageModel,Long> {
}
