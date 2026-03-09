package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskProgressLogEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskProgressLogMapper extends BaseMapper<TaskProgressLogEntity> {

    @Select("""
            SELECT COALESCE(MAX(step_no), 0)
            FROM task_progress_log
            WHERE task_id = #{taskId} AND student_id = #{studentId}
            """)
    Integer findMaxStepNo(Long taskId, Long studentId);

    @Select("""
            SELECT id, task_id, student_id, step_no, content, created_at
            FROM task_progress_log
            WHERE task_id = #{taskId} AND student_id = #{studentId}
            ORDER BY step_no ASC
            """)
    List<TaskProgressLogEntity> findByTaskAndStudent(Long taskId, Long studentId);

    @Select("""
            SELECT COUNT(1)
            FROM task_progress_log
            WHERE task_id = #{taskId} AND student_id = #{studentId}
            """)
    Integer countByTaskAndStudent(Long taskId, Long studentId);

    @Select("""
            SELECT MAX(created_at)
            FROM task_progress_log
            WHERE task_id = #{taskId} AND student_id = #{studentId}
            """)
    LocalDateTime findLatestCreatedAt(Long taskId, Long studentId);
}
