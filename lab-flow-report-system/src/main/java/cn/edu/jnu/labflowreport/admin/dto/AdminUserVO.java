package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserVO(
        Long id,
        String username,
        String displayName,
        Boolean enabled,
        Long departmentId,
        String departmentName,
        Long classId,
        String className,
        List<String> roleCodes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

