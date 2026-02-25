package cn.edu.jnu.labflowreport.attendance.dto;

public record AttendanceSessionCreateRequest(
        Long scheduleId,
        Long semesterId,
        Long classId,
        Integer tokenTtlSeconds
) {
}
