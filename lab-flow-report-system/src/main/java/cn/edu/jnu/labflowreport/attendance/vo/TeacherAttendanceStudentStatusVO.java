package cn.edu.jnu.labflowreport.attendance.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeacherAttendanceStudentStatusVO {
    private Long studentId;
    private String studentUsername;
    private String studentDisplayName;
    private String status;
    private String method;
    private LocalDateTime checkedInAt;
}
