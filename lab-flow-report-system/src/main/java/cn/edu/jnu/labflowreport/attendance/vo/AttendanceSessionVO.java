package cn.edu.jnu.labflowreport.attendance.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AttendanceSessionVO {
    private Long id;
    private Long scheduleId;
    private Long semesterId;
    private Long classId;
    private Long teacherId;
    private String status;
    private Integer tokenTtlSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
