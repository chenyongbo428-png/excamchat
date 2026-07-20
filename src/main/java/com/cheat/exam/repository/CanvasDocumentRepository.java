package com.cheat.exam.repository;

import com.cheat.exam.domain.canvas.CanvasDocument;
import com.cheat.exam.domain.session.ChatSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanvasDocumentRepository extends JpaRepository<CanvasDocument, Long> {

    Optional<CanvasDocument> findBySession(ChatSession session);
}
