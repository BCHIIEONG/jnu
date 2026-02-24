package cn.edu.jnu.labflowreport.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("attendance_record")
public class AttendanceRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long studentId;
    private String method;
    private LocalDateTime checkedInAt;
    private String ip;
    private String userAgent;
    private Long operatorId;
    private LocalDateTime createdAt;
}

