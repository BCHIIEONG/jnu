package cn.edu.jnu.labflowreport.flow.vo;

public record TaskDeviceConfigVO(
        Long deviceId,
        String deviceCode,
        String deviceName,
        String deviceStatus,
        Integer totalQuantity,
        Integer configuredQuantity,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}
