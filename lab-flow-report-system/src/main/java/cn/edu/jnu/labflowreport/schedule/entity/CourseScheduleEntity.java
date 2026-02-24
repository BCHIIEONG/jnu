package cn.edu.jnu.labflowreport.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("course_schedule")
public class CourseScheduleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private Long classId;
    private Long teacherId;
    private Long labRoomId;
    private LocalDate lessonDate;
    private Long slotId;
    private String courseName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

