package com.cheat.exam.domain.session;

import com.cheat.exam.common.jpa.BaseEntity;
import com.cheat.exam.domain.image.ImageResource;
import com.cheat.exam.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "chat_session")
public class ChatSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String title;

    @Column(name = "model_code", nullable = false, length = 64)
    private String modelCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageResource image;

    @Column(name = "subject_code", length = 32)
    private String subjectCode;

    @Column(name = "grade_level", length = 32)
    private String gradeLevel;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "guidance_state_json")
    private String guidanceStateJson;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public ImageResource getImage() {
        return image;
    }

    public void setImage(ImageResource image) {
        this.image = image;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getGuidanceStateJson() {
        return guidanceStateJson;
    }

    public void setGuidanceStateJson(String guidanceStateJson) {
        this.guidanceStateJson = guidanceStateJson;
    }
}
