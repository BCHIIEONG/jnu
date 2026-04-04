package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseTargetStudentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperimentCourseTargetStudentMapper extends BaseMapper<ExperimentCourseTargetStudentEntity> {

    @Select("SELECT student_id FROM experiment_course_target_student WHERE course_id = #{courseId} ORDER BY student_id ASC")
    List<Long> findStudentIdsByCourseId(Long courseId);
}
