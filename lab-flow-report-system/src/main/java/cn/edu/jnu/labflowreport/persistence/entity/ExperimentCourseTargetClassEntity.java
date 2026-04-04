package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("experiment_course_target_class")
public class ExperimentCourseTargetClassEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long classId;
}
