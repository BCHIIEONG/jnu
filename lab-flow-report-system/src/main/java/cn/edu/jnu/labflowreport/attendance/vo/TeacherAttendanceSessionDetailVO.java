package cn.edu.jnu.labflowreport.attendance.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class TeacherAttendanceSessionDetailVO {
    private Long sessionId;
    private String sourceType;
    private String courseName;
    private Long classId;
    private String classDisplayName;
    private Integer grade;
    private Long experimentCourseId;
    private Long experimentCourseSlotId;
    private Long experimentCourseInstanceId;
    private String labRoomName;
    private LocalDate lessonDate;
    private String slotName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private long checkedInCount;
    private long absentCount;
    private long totalCount;
    private List<TeacherAttendanceStudentStatusVO> roster;
}
