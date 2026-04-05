package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseEnrollmentRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseSlotRowVO;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseSlotEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperimentCourseSlotMapper extends BaseMapper<ExperimentCourseSlotEntity> {

    @Select("SELECT * FROM experiment_course_slot WHERE id = #{slotId} LIMIT 1 FOR UPDATE")
    ExperimentCourseSlotEntity findByIdForUpdate(Long slotId);

    @Select("""
            <script>
            SELECT ecs.id,
                   ecs.course_id,
                   ecs.name,
                   ecs.mode,
                   ecs.first_lesson_date,
                   ecs.repeat_pattern,
                   ecs.range_mode,
                   ecs.range_start_date,
                   ecs.range_end_date,
                   ecs.slot_id,
                   ts.code AS slot_code,
                   ts.name AS slot_name,
                   ts.start_time AS slot_start_time,
                   ts.end_time AS slot_end_time,
                   ecs.lab_room_id,
                   lr.name AS lab_room_name,
                   ecs.capacity,
                   COALESCE(en.enrolled_count, 0) AS enrolled_count
            FROM experiment_course_slot ecs
            JOIN time_slot ts ON ts.id = ecs.slot_id
            JOIN lab_room lr ON lr.id = ecs.lab_room_id
            LEFT JOIN (
                SELECT slot_id, COUNT(*) AS enrolled_count
                FROM experiment_course_enrollment
                WHERE status = 'ENROLLED'
                GROUP BY slot_id
            ) en ON en.slot_id = ecs.id
            WHERE ecs.course_id IN
            <foreach collection="courseIds" item="courseId" open="(" separator="," close=")">
                #{courseId}
            </foreach>
            ORDER BY ecs.first_lesson_date ASC, ts.start_time ASC, ecs.id ASC
            </script>
            """)
    List<ExperimentCourseSlotRowVO> findRowsByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Select("""
            SELECT ec.id AS course_id,
                   ec.title AS course_title,
                   ecs.id AS slot_id,
                   ecs.first_lesson_date AS lesson_date,
                   COALESCE(NULLIF(TRIM(ecs.name), ''), CONCAT('场次', ecs.id)) AS slot_name,
                   lr.name AS lab_room_name,
                   ecs.capacity,
                   u.id AS student_id,
                   u.username AS student_username,
                   u.display_name AS student_display_name,
                   CASE WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name) ELSE c.name END AS class_display_name,
                   e.selected_at
            FROM experiment_course_enrollment e
            JOIN experiment_course ec ON ec.id = e.course_id
            JOIN experiment_course_slot ecs ON ecs.id = e.slot_id
            JOIN lab_room lr ON lr.id = ecs.lab_room_id
            JOIN sys_user u ON u.id = e.student_id
            LEFT JOIN org_class c ON c.id = u.class_id
            WHERE e.course_id = #{courseId}
              AND e.status = 'ENROLLED'
            ORDER BY ecs.first_lesson_date ASC, e.selected_at ASC, u.username ASC
            """)
    List<ExperimentCourseEnrollmentRowVO> findEnrollmentRowsByCourseId(Long courseId);
}
