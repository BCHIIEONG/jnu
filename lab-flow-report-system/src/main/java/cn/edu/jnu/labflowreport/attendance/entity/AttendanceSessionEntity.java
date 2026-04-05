package cn.edu.jnu.labflowreport.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("attendance_session")
public class AttendanceSessionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceType;
    private Long scheduleId;
    private Long semesterId;
    private Long classId;
    private Long teacherId;
    private Long experimentCourseId;
    private Long experimentCourseSlotId;
    private Long experimentCourseInstanceId;
    private String status;
    private String staticCode;
    private Integer tokenTtlSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
