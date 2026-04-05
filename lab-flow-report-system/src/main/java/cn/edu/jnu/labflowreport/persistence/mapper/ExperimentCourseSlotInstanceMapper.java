package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseSlotInstanceRowVO;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseSlotInstanceEntity;
import cn.edu.jnu.labflowreport.schedule.vo.TeacherWeekScheduleItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperimentCourseSlotInstanceMapper extends BaseMapper<ExperimentCourseSlotInstanceEntity> {

    @Select("""
            <script>
            SELECT ecsi.id,
                   ecsi.course_id,
                   ecsi.slot_group_id,
                   ecsi.lesson_date,
                   ecsi.teaching_week,
                   ecsi.display_name,
                   ecsi.slot_id,
                   ts.code AS slot_code,
                   ts.name AS slot_name,
                   ts.start_time AS slot_start_time,
                   ts.end_time AS slot_end_time,
                   ecsi.lab_room_id,
                   lr.name AS lab_room_name,
                   ecsi.capacity
            FROM experiment_course_slot_instance ecsi
            JOIN time_slot ts ON ts.id = ecsi.slot_id
            JOIN lab_room lr ON lr.id = ecsi.lab_room_id
            WHERE ecsi.course_id IN
            <foreach collection="courseIds" item="courseId" open="(" separator="," close=")">
                #{courseId}
            </foreach>
            ORDER BY ecsi.lesson_date ASC, ts.start_time ASC, ecsi.id ASC
            </script>
            """)
    List<ExperimentCourseSlotInstanceRowVO> findRowsByCourseIds(@Param("courseIds") List<Long> courseIds);

    @Select("""
            SELECT ecsi.id,
                   'EXPERIMENT_COURSE' AS source_type,
                   ec.id AS experiment_course_id,
                   ecsi.slot_group_id AS experiment_course_slot_id,
                   ecsi.id AS experiment_course_instance_id,
                   ec.semester_id,
                   NULL AS class_id,
                   COALESCE(NULLIF(TRIM(ecs.name), ''), CONCAT('场次', ecs.id)) AS class_name,
                   ec.teacher_id,
                   tu.display_name AS teacher_display_name,
                   ecsi.lab_room_id,
                   lr.name AS lab_room_name,
                   ecsi.lesson_date,
                   ecsi.slot_id,
                   ts.code AS slot_code,
                   ecsi.display_name AS slot_name,
                   ts.start_time AS slot_start_time,
                   ts.end_time AS slot_end_time,
                   ec.title AS course_name
            FROM experiment_course_slot_instance ecsi
            JOIN experiment_course ec ON ec.id = ecsi.course_id
            JOIN experiment_course_slot ecs ON ecs.id = ecsi.slot_group_id
            JOIN sys_user tu ON tu.id = ec.teacher_id
            JOIN time_slot ts ON ts.id = ecsi.slot_id
            JOIN lab_room lr ON lr.id = ecsi.lab_room_id
            WHERE ec.teacher_id = #{teacherId}
              AND ec.semester_id = #{semesterId}
              AND ecsi.lesson_date BETWEEN #{from} AND #{to}
            ORDER BY ecsi.lesson_date ASC, ts.start_time ASC, ecsi.id ASC
            """)
    List<TeacherWeekScheduleItemVO> findTeacherWeekRows(
            @Param("teacherId") Long teacherId,
            @Param("semesterId") Long semesterId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
