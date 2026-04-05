package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.elective.vo.StudentEnrollmentRowVO;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseEnrollmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperimentCourseEnrollmentMapper extends BaseMapper<ExperimentCourseEnrollmentEntity> {

    @Select("SELECT * FROM experiment_course_enrollment WHERE course_id = #{courseId} AND student_id = #{studentId} LIMIT 1")
    ExperimentCourseEnrollmentEntity findByCourseAndStudent(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    @Select("SELECT * FROM experiment_course_enrollment WHERE course_id = #{courseId} AND student_id = #{studentId} LIMIT 1 FOR UPDATE")
    ExperimentCourseEnrollmentEntity findByCourseAndStudentForUpdate(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM experiment_course_enrollment WHERE slot_id = #{slotId} AND status = 'ENROLLED'")
    Integer countEnrolledBySlotId(Long slotId);

    @Select("SELECT COUNT(*) FROM experiment_course_enrollment WHERE course_id = #{courseId} AND status = 'ENROLLED'")
    Integer countActiveByCourse(Long courseId);

    @Select("SELECT * FROM experiment_course_enrollment WHERE student_id = #{studentId} AND status = 'ENROLLED' ORDER BY selected_at DESC, id DESC")
    List<ExperimentCourseEnrollmentEntity> findActiveByStudentId(Long studentId);

    @Select("""
            SELECT e.id AS enrollment_id,
                   e.course_id,
                   ec.title AS course_title,
                   ec.description AS course_description,
                   ec.teacher_id,
                   tu.display_name AS teacher_display_name,
                   ec.semester_id,
                   se.name AS semester_name,
                   ec.status,
                   ec.enroll_deadline_at,
                   e.slot_id,
                   COALESCE(NULLIF(TRIM(ecs.name), ''), CONCAT('场次', ecs.id)) AS slot_group_name,
                   ecs.mode AS slot_mode,
                   ecs.first_lesson_date,
                   ecs.repeat_pattern,
                   ecs.range_mode,
                   ecs.range_start_date,
                   ecs.range_end_date,
                   e.selected_at
            FROM experiment_course_enrollment e
            JOIN experiment_course ec ON ec.id = e.course_id
            JOIN sys_user tu ON tu.id = ec.teacher_id
            JOIN semester se ON se.id = ec.semester_id
            JOIN experiment_course_slot ecs ON ecs.id = e.slot_id
            WHERE e.student_id = #{studentId}
              AND e.status = 'ENROLLED'
            ORDER BY ecs.first_lesson_date ASC, e.selected_at DESC, e.id DESC
            """)
    List<StudentEnrollmentRowVO> findActiveRowsByStudentId(Long studentId);

    @Select("""
            SELECT e.id AS enrollment_id,
                   e.course_id,
                   ec.title AS course_title,
                   ec.teacher_id,
                   tu.display_name AS teacher_display_name,
                   ec.semester_id,
                   ecsi.slot_group_id AS slot_id,
                   ecsi.id AS instance_id,
                   ecsi.display_name AS instance_display_name,
                   ecsi.teaching_week,
                   ecsi.lesson_date,
                   ts.code AS slot_code,
                   ts.name AS slot_name,
                   ts.start_time AS slot_start_time,
                   ts.end_time AS slot_end_time,
                   ecsi.lab_room_id,
                   lr.name AS lab_room_name
            FROM experiment_course_enrollment e
            JOIN experiment_course ec ON ec.id = e.course_id
            JOIN sys_user tu ON tu.id = ec.teacher_id
            JOIN experiment_course_slot_instance ecsi ON ecsi.slot_group_id = e.slot_id
            JOIN time_slot ts ON ts.id = ecsi.slot_id
            JOIN lab_room lr ON lr.id = ecsi.lab_room_id
            WHERE e.student_id = #{studentId}
              AND e.status = 'ENROLLED'
              AND ecsi.lesson_date BETWEEN #{from} AND #{to}
            ORDER BY ecsi.lesson_date ASC, ts.start_time ASC, ecsi.id ASC
            """)
    List<StudentEnrollmentRowVO.ScheduleRow> findActiveScheduleRowsByStudentId(
            @Param("studentId") Long studentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Select("SELECT COUNT(*) FROM experiment_course_enrollment WHERE course_id = #{courseId} AND student_id = #{studentId} AND status = 'ENROLLED'")
    Integer countActiveByCourseAndStudent(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM experiment_course_enrollment WHERE slot_id = #{slotId} AND student_id = #{studentId} AND status = 'ENROLLED'")
    Integer countActiveBySlotAndStudent(@Param("slotId") Long slotId, @Param("studentId") Long studentId);

    @Select("SELECT * FROM experiment_course_enrollment WHERE course_id = #{courseId} AND status = 'ENROLLED' ORDER BY selected_at ASC, id ASC")
    List<ExperimentCourseEnrollmentEntity> findActiveByCourseId(@Param("courseId") Long courseId);

    @Select("""
            SELECT DISTINCT u.id,
                   u.username,
                   u.display_name,
                   u.class_id
            FROM experiment_course_enrollment e
            JOIN sys_user u ON u.id = e.student_id
            WHERE e.slot_id = #{slotId}
              AND e.status = 'ENROLLED'
              AND u.enabled = TRUE
            ORDER BY u.username ASC
            """)
    List<SysUserEntity> findActiveStudentsBySlotId(@Param("slotId") Long slotId);
}
