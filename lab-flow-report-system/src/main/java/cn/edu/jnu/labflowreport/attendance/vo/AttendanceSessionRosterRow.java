package cn.edu.jnu.labflowreport.attendance.vo;

public record AttendanceSessionRosterRow(
        Long studentId,
        String studentUsername,
        String studentDisplayName,
        Long classId
) {
}
