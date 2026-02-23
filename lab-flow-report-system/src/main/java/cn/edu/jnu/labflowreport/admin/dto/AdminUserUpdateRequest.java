package cn.edu.jnu.labflowreport.admin.dto;

public record AdminUserUpdateRequest(
        String displayName,
        Boolean enabled,
        Long departmentId,
        Long classId
) {
}

