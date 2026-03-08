package cn.edu.jnu.labflowreport.attendance.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TeacherAttendanceSessionListItemVO {
    private Long sessionId;
    private String courseName;
    private Long classId;
    private String classDisplayName;
    private Integer grade;
    private String labRoomName;
    private LocalDate lessonDate;
    private String slotName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private long checkedInCount;
    private long totalCount;
    private long absentCount;
}
