package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("experiment_course_enrollment")
public class ExperimentCourseEnrollmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long slotId;
    private Long studentId;
    private String status;
    private LocalDateTime selectedAt;
}
