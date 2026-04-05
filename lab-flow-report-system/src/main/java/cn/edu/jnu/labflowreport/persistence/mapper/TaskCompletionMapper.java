package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskCompletionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskCompletionMapper extends BaseMapper<TaskCompletionEntity> {

    @Select("""
            SELECT id, task_id, student_id, status, completion_source, requested_at, confirmed_at, confirmed_by
            FROM task_completion
            WHERE task_id = #{taskId} AND student_id = #{studentId}
            LIMIT 1
            """)
    TaskCompletionEntity findByTaskAndStudent(Long taskId, Long studentId);
}
