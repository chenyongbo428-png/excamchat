package com.cheat.exam.domain.canvas;

import com.cheat.exam.domain.message.ChatMessage;
import com.cheat.exam.domain.session.ChatSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "canvas_operation")
@EntityListeners(AuditingEntityListener.class)
public class CanvasOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private ChatMessage message;

    @Column(name = "operator_type", nullable = false, length = 32)
    private String operatorType;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operation_type", nullable = false, length = 64)
    private String operationType;

    @Column(name = "layer_type", nullable = false, length = 32)
    private String layerType;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "sequence_no", nullable = false)
    private long sequenceNo;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public void setMessage(ChatMessage message) {
        this.message = message;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getLayerType() {
        return layerType;
    }

    public void setLayerType(String layerType) {
        this.layerType = layerType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
