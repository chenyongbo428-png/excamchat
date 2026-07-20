package com.cheat.exam.repository;

import com.cheat.exam.domain.session.ChatSession;
import com.cheat.exam.domain.user.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Page<ChatSession> findByUserAndStatus(User user, String status, Pageable pageable);

    Optional<ChatSession> findByIdAndUserAndStatus(Long id, User user, String status);
}
