package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;

public record AdminAuditLogVO(
        Long id,
        Long actorId,
        String actorUsername,
        String action,
        String targetType,
        Long targetId,
        String detailJson,
        LocalDateTime createdAt
) {
}

