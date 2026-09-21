package com.carservice.backend.admin.dto;

import java.util.List;

public class AdminRoleResponse {

    private String role;
    private String name;
    private String description;
    private Boolean isPrivileged;
    private long assignedUsersCount;
    private List<PermissionDto> permissions;

    public AdminRoleResponse() {
    }

    public AdminRoleResponse(
            String role,
            String name,
            String description,
            Boolean isPrivileged,
            long assignedUsersCount,
            List<PermissionDto> permissions
    ) {
        this.role = role;
        this.name = name;
        this.description = description;
        this.isPrivileged = isPrivileged;
        this.assignedUsersCount = assignedUsersCount;
        this.permissions = permissions;
    }

    public static class PermissionDto {
        private String code;
        private String module;
        private String description;

        public PermissionDto() {
        }

        public PermissionDto(String code, String module, String description) {
            this.code = code;
            this.module = module;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsPrivileged() {
        return isPrivileged;
    }

    public void setIsPrivileged(Boolean isPrivileged) {
        this.isPrivileged = isPrivileged;
    }

    public boolean isPrivileged() {
        return Boolean.TRUE.equals(isPrivileged);
    }

    public long getAssignedUsersCount() {
        return assignedUsersCount;
    }

    public void setAssignedUsersCount(long assignedUsersCount) {
        this.assignedUsersCount = assignedUsersCount;
    }

    public List<PermissionDto> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDto> permissions) {
        this.permissions = permissions;
    }
}
