package com.cheat.exam.repository;

import com.cheat.exam.domain.message.ChatMessage;
import com.cheat.exam.domain.session.ChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session);
}
