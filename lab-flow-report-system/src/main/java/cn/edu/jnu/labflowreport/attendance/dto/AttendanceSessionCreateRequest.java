package cn.edu.jnu.labflowreport.attendance.dto;

public record AttendanceSessionCreateRequest(
        Long scheduleId,
        Long experimentCourseInstanceId,
        Long semesterId,
        Long classId,
        Integer tokenTtlSeconds
) {
}
