package cn.edu.jnu.labflowreport.attendance.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AttendanceRecordVO {
    private Long id;
    private Long sessionId;
    private Long studentId;
    private String studentUsername;
    private String studentDisplayName;
    private String method;
    private LocalDateTime checkedInAt;
    private Long operatorId;
}

