package com.cheat.exam.repository;

import com.cheat.exam.domain.canvas.CanvasOperation;
import com.cheat.exam.domain.session.ChatSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CanvasOperationRepository extends JpaRepository<CanvasOperation, Long> {

    @Query("select coalesce(max(operation.sequenceNo), 0) from CanvasOperation operation where operation.session = :session")
    long findMaxSequenceNoBySession(@Param("session") ChatSession session);

    List<CanvasOperation> findBySessionOrderBySequenceNoAsc(ChatSession session);
}
