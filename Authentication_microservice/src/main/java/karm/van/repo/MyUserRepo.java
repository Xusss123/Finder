package karm.van.repo;

import karm.van.model.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface MyUserRepo extends JpaRepository<MyUser, Long> {

    Optional<MyUser> findByName(String name);

    boolean existsByName(String name);
    boolean existsByEmail(String email);

    void deleteByName(String name);

    @Modifying
    @Query("UPDATE MyUser user " +
            "SET user.isEnable = :enable " +
            "WHERE user.id = :userId")
    void updateUserStatus(@Param("userId") Long userId, @Param("enable") boolean enable);
}
