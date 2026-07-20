package com.cheat.exam.repository;

import com.cheat.exam.domain.image.ImageResource;
import com.cheat.exam.domain.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageResourceRepository extends JpaRepository<ImageResource, Long> {

    Optional<ImageResource> findByIdAndUser(Long id, User user);
}
