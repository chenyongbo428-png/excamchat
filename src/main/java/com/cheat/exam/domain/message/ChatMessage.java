package com.cheat.exam.domain.message;

import com.cheat.exam.common.jpa.BaseEntity;
import com.cheat.exam.domain.session.ChatSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_message")
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content_text")
    private String contentText;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "raw_payload_json")
    private String rawPayloadJson;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "annotation_json")
    private String annotationJson;

    @Column(name = "hint_level")
    private Integer hintLevel;

    @Column(name = "guidance_stage", length = 32)
    private String guidanceStage;

    @Column(name = "message_status", nullable = false, length = 32)
    private String messageStatus;

    @Column(name = "token_usage_prompt")
    private Integer tokenUsagePrompt;

    @Column(name = "token_usage_completion")
    private Integer tokenUsageCompletion;

    @Column(name = "provider_request_id", length = 128)
    private String providerRequestId;

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getRawPayloadJson() {
        return rawPayloadJson;
    }

    public void setRawPayloadJson(String rawPayloadJson) {
        this.rawPayloadJson = rawPayloadJson;
    }

    public String getAnnotationJson() {
        return annotationJson;
    }

    public void setAnnotationJson(String annotationJson) {
        this.annotationJson = annotationJson;
    }

    public Integer getHintLevel() {
        return hintLevel;
    }

    public void setHintLevel(Integer hintLevel) {
        this.hintLevel = hintLevel;
    }

    public String getGuidanceStage() {
        return guidanceStage;
    }

    public void setGuidanceStage(String guidanceStage) {
        this.guidanceStage = guidanceStage;
    }

    public String getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(String messageStatus) {
        this.messageStatus = messageStatus;
    }

    public Integer getTokenUsagePrompt() {
        return tokenUsagePrompt;
    }

    public void setTokenUsagePrompt(Integer tokenUsagePrompt) {
        this.tokenUsagePrompt = tokenUsagePrompt;
    }

    public Integer getTokenUsageCompletion() {
        return tokenUsageCompletion;
    }

    public void setTokenUsageCompletion(Integer tokenUsageCompletion) {
        this.tokenUsageCompletion = tokenUsageCompletion;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public void setProviderRequestId(String providerRequestId) {
        this.providerRequestId = providerRequestId;
    }
}
