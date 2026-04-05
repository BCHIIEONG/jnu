package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("experiment_course_slot_instance")
public class ExperimentCourseSlotInstanceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long slotGroupId;
    private LocalDate lessonDate;
    private Integer teachingWeek;
    private String displayName;
    private Long slotId;
    private Long labRoomId;
    private Integer capacity;
    private LocalDateTime createdAt;
}
