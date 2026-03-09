package cn.edu.jnu.labflowreport.admin.dto;

public record AdminUserUpdateRequest(
        String username,
        String displayName,
        Boolean enabled,
        Long departmentId,
        Long classId,
        java.util.List<Long> classIds
) {
}
