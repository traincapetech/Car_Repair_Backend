package com.carservice.backend.admin.dto;

import com.carservice.backend.admin.entity.AuditLog;

public class AdminAuditLogDetailResponse extends AdminAuditLogResponse {

    private String beforeStateJson;
    private String afterStateJson;
    private String diffJson;
    private String metadataJson;

    public AdminAuditLogDetailResponse() {
        super();
    }

    public AdminAuditLogDetailResponse(AuditLog log) {
        super(log);
        if (log != null) {
            this.beforeStateJson = log.getBeforeStateJson();
            this.afterStateJson = log.getAfterStateJson();
            this.diffJson = log.getDiffJson();
            this.metadataJson = log.getMetadataJson();
        }
    }

    public String getBeforeStateJson() {
        return beforeStateJson;
    }

    public void setBeforeStateJson(String beforeStateJson) {
        this.beforeStateJson = beforeStateJson;
    }

    public String getAfterStateJson() {
        return afterStateJson;
    }

    public void setAfterStateJson(String afterStateJson) {
        this.afterStateJson = afterStateJson;
    }

    public String getDiffJson() {
        return diffJson;
    }

    public void setDiffJson(String diffJson) {
        this.diffJson = diffJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
