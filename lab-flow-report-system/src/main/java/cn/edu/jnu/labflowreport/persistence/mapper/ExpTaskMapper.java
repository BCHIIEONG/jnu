package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskEntity;
import cn.edu.jnu.labflowreport.workflow.vo.TaskVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExpTaskMapper extends BaseMapper<ExpTaskEntity> {

    @Select("""
            SELECT t.id,
                   t.title,
                   t.description,
                   t.publisher_id,
                   u.display_name AS publisher_name,
                   t.experiment_course_id,
                   ec.title AS experiment_course_title,
                   t.deadline_at,
                   t.status,
                   t.created_at
            FROM exp_task t
            JOIN sys_user u ON u.id = t.publisher_id
            LEFT JOIN experiment_course ec ON ec.id = t.experiment_course_id
            ORDER BY t.created_at DESC
            """)
    List<TaskVO> findTaskList();

    @Select("""
            SELECT t.id,
                   t.title,
                   t.description,
                   t.publisher_id,
                   u.display_name AS publisher_name,
                   t.experiment_course_id,
                   ec.title AS experiment_course_title,
                   t.deadline_at,
                   t.status,
                   t.created_at
            FROM exp_task t
            JOIN sys_user u ON u.id = t.publisher_id
            LEFT JOIN experiment_course ec ON ec.id = t.experiment_course_id
            WHERE t.publisher_id = #{publisherId}
            ORDER BY t.created_at DESC
            """)
    List<TaskVO> findTaskListForTeacher(Long publisherId);

    @Select("""
            SELECT t.id,
                   t.title,
                   t.description,
                   t.publisher_id,
                   u.display_name AS publisher_name,
                   t.experiment_course_id,
                   ec.title AS experiment_course_title,
                   t.deadline_at,
                   t.status,
                   t.created_at
            FROM exp_task t
            JOIN sys_user u ON u.id = t.publisher_id
            LEFT JOIN experiment_course ec ON ec.id = t.experiment_course_id
            WHERE (
                (
                    t.experiment_course_id IS NULL
                    AND NOT EXISTS (SELECT 1 FROM exp_task_target_class tc WHERE tc.task_id = t.id)
                )
                OR (
                    t.experiment_course_id IS NULL
                    AND EXISTS (
                    SELECT 1
                    FROM exp_task_target_class tc
                    JOIN sys_user su ON su.id = #{studentId}
                    WHERE tc.task_id = t.id
                      AND su.class_id IS NOT NULL
                      AND tc.class_id = su.class_id
                )
                )
                OR (
                    t.experiment_course_id IS NOT NULL
                    AND EXISTS (
                    SELECT 1
                    FROM experiment_course_enrollment e
                    WHERE e.course_id = t.experiment_course_id
                      AND e.student_id = #{studentId}
                      AND e.status = 'ENROLLED'
                )
                )
            )
            ORDER BY t.created_at DESC
            """)
    List<TaskVO> findTaskListForStudent(Long studentId);

    @Select("""
            SELECT t.id,
                   t.title,
                   t.description,
                   t.publisher_id,
                   u.display_name AS publisher_name,
                   t.experiment_course_id,
                   ec.title AS experiment_course_title,
                   t.deadline_at,
                   t.status,
                   t.created_at
            FROM exp_task t
            JOIN sys_user u ON u.id = t.publisher_id
            LEFT JOIN experiment_course ec ON ec.id = t.experiment_course_id
            WHERE t.id = #{taskId}
            """)
    TaskVO findTaskById(Long taskId);

    @Select("SELECT publisher_id FROM exp_task WHERE id = #{taskId}")
    Long findPublisherId(Long taskId);

    @Select("""
            SELECT COUNT(*)
            FROM exp_task t
            JOIN sys_user su ON su.id = #{studentId}
            WHERE t.id = #{taskId}
              AND (
                  (
                      t.experiment_course_id IS NULL
                      AND NOT EXISTS (SELECT 1 FROM exp_task_target_class tc WHERE tc.task_id = t.id)
                  )
                  OR (
                      t.experiment_course_id IS NULL
                      AND EXISTS (
                      SELECT 1
                      FROM exp_task_target_class tc
                      WHERE tc.task_id = t.id
                        AND su.class_id IS NOT NULL
                        AND tc.class_id = su.class_id
                  )
                  )
                  OR (
                      t.experiment_course_id IS NOT NULL
                      AND EXISTS (
                      SELECT 1
                      FROM experiment_course_enrollment e
                      WHERE e.course_id = t.experiment_course_id
                        AND e.student_id = #{studentId}
                        AND e.status = 'ENROLLED'
                  )
                  )
              )
            """)
    Integer countStudentAccessibleTask(Long taskId, Long studentId);
}
