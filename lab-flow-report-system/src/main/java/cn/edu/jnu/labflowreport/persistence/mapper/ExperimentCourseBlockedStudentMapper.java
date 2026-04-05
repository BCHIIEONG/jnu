package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseBlockedStudentVO;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseBlockedStudentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperimentCourseBlockedStudentMapper extends BaseMapper<ExperimentCourseBlockedStudentEntity> {

    @Select("SELECT * FROM experiment_course_blocked_student WHERE course_id = #{courseId} AND student_id = #{studentId} LIMIT 1")
    ExperimentCourseBlockedStudentEntity findByCourseAndStudent(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    @Select("""
            SELECT b.course_id
            FROM experiment_course_blocked_student b
            WHERE b.student_id = #{studentId}
            """)
    List<Long> findCourseIdsByStudentId(@Param("studentId") Long studentId);

    @Select("""
            SELECT u.id AS student_id,
                   u.username AS student_username,
                   u.display_name AS student_display_name,
                   CASE WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name) ELSE c.name END AS class_display_name,
                   b.blocked_at
            FROM experiment_course_blocked_student b
            JOIN sys_user u ON u.id = b.student_id
            LEFT JOIN org_class c ON c.id = u.class_id
            WHERE b.course_id = #{courseId}
            ORDER BY b.blocked_at DESC, u.username ASC
            """)
    List<ExperimentCourseBlockedStudentVO> findRowsByCourseId(@Param("courseId") Long courseId);
}
