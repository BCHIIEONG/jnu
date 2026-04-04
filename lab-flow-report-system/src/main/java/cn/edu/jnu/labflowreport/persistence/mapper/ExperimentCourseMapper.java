package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseSummaryRowVO;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperimentCourseMapper extends BaseMapper<ExperimentCourseEntity> {

    @Select("SELECT * FROM experiment_course WHERE id = #{courseId} LIMIT 1 FOR UPDATE")
    ExperimentCourseEntity findByIdForUpdate(Long courseId);

    @Select("""
            SELECT ec.id,
                   ec.title,
                   ec.description,
                   ec.teacher_id,
                   t.display_name AS teacher_display_name,
                   ec.semester_id,
                   s.name AS semester_name,
                   ec.status,
                   ec.enroll_deadline_at,
                   ec.created_at,
                   ec.updated_at
            FROM experiment_course ec
            JOIN sys_user t ON t.id = ec.teacher_id
            JOIN semester s ON s.id = ec.semester_id
            WHERE ec.teacher_id = #{teacherId}
            ORDER BY ec.created_at DESC, ec.id DESC
            """)
    List<ExperimentCourseSummaryRowVO> findTeacherCourseSummaries(Long teacherId);

    @Select("""
            SELECT ec.id,
                   ec.title,
                   ec.description,
                   ec.teacher_id,
                   t.display_name AS teacher_display_name,
                   ec.semester_id,
                   s.name AS semester_name,
                   ec.status,
                   ec.enroll_deadline_at,
                   ec.created_at,
                   ec.updated_at
            FROM experiment_course ec
            JOIN sys_user t ON t.id = ec.teacher_id
            JOIN semester s ON s.id = ec.semester_id
            WHERE ec.id = #{courseId}
            LIMIT 1
            """)
    ExperimentCourseSummaryRowVO findSummaryById(Long courseId);

    @Select("""
            <script>
            SELECT ec.id,
                   ec.title,
                   ec.description,
                   ec.teacher_id,
                   t.display_name AS teacher_display_name,
                   ec.semester_id,
                   s.name AS semester_name,
                   ec.status,
                   ec.enroll_deadline_at,
                   ec.created_at,
                   ec.updated_at
            FROM experiment_course ec
            JOIN sys_user t ON t.id = ec.teacher_id
            JOIN semester s ON s.id = ec.semester_id
            WHERE ec.status = 'OPEN'
            <if test="semesterId != null"> AND ec.semester_id = #{semesterId}</if>
            ORDER BY ec.enroll_deadline_at ASC, ec.created_at DESC, ec.id DESC
            </script>
            """)
    List<ExperimentCourseSummaryRowVO> findOpenCourseSummaries(@Param("semesterId") Long semesterId);
}
