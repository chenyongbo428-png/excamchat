package com.cheat.exam.domain.model;

import com.cheat.exam.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "model_config")
public class ModelConfig extends BaseEntity {

    @Column(name = "model_code", nullable = false, unique = true, length = 64)
    private String modelCode;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "supports_vision", nullable = false)
    private boolean supportsVision;

    @Column(name = "supports_stream", nullable = false)
    private boolean supportsStream;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "config_json")
    private String configJson;

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public boolean isSupportsVision() {
        return supportsVision;
    }

    public void setSupportsVision(boolean supportsVision) {
        this.supportsVision = supportsVision;
    }

    public boolean isSupportsStream() {
        return supportsStream;
    }

    public void setSupportsStream(boolean supportsStream) {
        this.supportsStream = supportsStream;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
}
