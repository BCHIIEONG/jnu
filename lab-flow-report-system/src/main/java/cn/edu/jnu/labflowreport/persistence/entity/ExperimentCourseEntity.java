package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("experiment_course")
public class ExperimentCourseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private Long semesterId;
    private String status;
    private LocalDateTime enrollDeadlineAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
