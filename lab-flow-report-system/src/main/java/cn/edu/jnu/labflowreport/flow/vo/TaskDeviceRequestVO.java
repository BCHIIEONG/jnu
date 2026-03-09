package cn.edu.jnu.labflowreport.flow.vo;

import java.time.LocalDateTime;

public record TaskDeviceRequestVO(
        Long id,
        Long taskId,
        Long studentId,
        String studentUsername,
        String studentDisplayName,
        Long deviceId,
        String deviceCode,
        String deviceName,
        Integer quantity,
        String status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime checkoutAt,
        LocalDateTime returnAt
) {
}
