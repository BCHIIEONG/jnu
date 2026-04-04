package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseTargetClassEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperimentCourseTargetClassMapper extends BaseMapper<ExperimentCourseTargetClassEntity> {

    @Select("SELECT class_id FROM experiment_course_target_class WHERE course_id = #{courseId} ORDER BY class_id ASC")
    List<Long> findClassIdsByCourseId(Long courseId);
}
