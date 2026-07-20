package com.cheat.exam.domain.canvas;

import com.cheat.exam.common.jpa.BaseEntity;
import com.cheat.exam.domain.image.ImageResource;
import com.cheat.exam.domain.session.ChatSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "canvas_document")
public class CanvasDocument extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "background_image_id", nullable = false)
    private ImageResource backgroundImage;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "snapshot_json", nullable = false)
    private String snapshotJson;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "updated_by_type", nullable = false, length = 32)
    private String updatedByType;

    @Column(name = "updated_by_id")
    private Long updatedById;

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public ImageResource getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(ImageResource backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public String getUpdatedByType() {
        return updatedByType;
    }

    public void setUpdatedByType(String updatedByType) {
        this.updatedByType = updatedByType;
    }

    public Long getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(Long updatedById) {
        this.updatedById = updatedById;
    }
}
