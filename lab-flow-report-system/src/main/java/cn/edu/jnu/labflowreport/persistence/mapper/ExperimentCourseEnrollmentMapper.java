package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.elective.vo.StudentEnrollmentRowVO;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseEnrollmentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExperimentCourseEnrollmentMapper extends BaseMapper<ExperimentCourseEnrollmentEntity> {

    @Select("SELECT * FROM experiment_course_enrollment WHERE course_id = #{courseId} AND student_id = #{studentId} LIMIT 1")
    ExperimentCourseEnrollmentEntity findByCourseAndStudent(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM experiment_course_enrollment WHERE slot_id = #{slotId} AND status = 'ENROLLED'")
    Integer countEnrolledBySlotId(Long slotId);

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
                   ecs.lesson_date,
                   ts.code AS slot_code,
                   ts.name AS slot_name,
                   ts.start_time AS slot_start_time,
                   ts.end_time AS slot_end_time,
                   ecs.lab_room_id,
                   lr.name AS lab_room_name,
                   ecs.capacity,
                   e.selected_at
            FROM experiment_course_enrollment e
            JOIN experiment_course ec ON ec.id = e.course_id
            JOIN sys_user tu ON tu.id = ec.teacher_id
            JOIN semester se ON se.id = ec.semester_id
            JOIN experiment_course_slot ecs ON ecs.id = e.slot_id
            JOIN time_slot ts ON ts.id = ecs.slot_id
            JOIN lab_room lr ON lr.id = ecs.lab_room_id
            WHERE e.student_id = #{studentId}
              AND e.status = 'ENROLLED'
            ORDER BY ecs.lesson_date ASC, ts.start_time ASC, e.selected_at DESC
            """)
    List<StudentEnrollmentRowVO> findActiveRowsByStudentId(Long studentId);

    @Select("SELECT COUNT(*) FROM experiment_course_enrollment WHERE course_id = #{courseId} AND student_id = #{studentId} AND status = 'ENROLLED'")
    Integer countActiveByCourseAndStudent(@Param("courseId") Long courseId, @Param("studentId") Long studentId);
}
