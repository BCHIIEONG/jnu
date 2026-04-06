package cn.edu.jnu.labflowreport.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("attendance_session_roster")
public class AttendanceSessionRosterEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long studentId;
    private Long classId;
    private LocalDateTime createdAt;
}
