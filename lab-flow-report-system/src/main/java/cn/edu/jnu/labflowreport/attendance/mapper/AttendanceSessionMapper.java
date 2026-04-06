package cn.edu.jnu.labflowreport.attendance.mapper;

import cn.edu.jnu.labflowreport.attendance.entity.AttendanceSessionEntity;
import cn.edu.jnu.labflowreport.attendance.vo.TeacherAttendanceSessionListItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceSessionMapper extends BaseMapper<AttendanceSessionEntity> {

    @Select("""
            SELECT id, source_type, schedule_id, semester_id, class_id, teacher_id,
                   experiment_course_id, experiment_course_slot_id, experiment_course_instance_id,
                   status, static_code, token_ttl_seconds, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE schedule_id = #{scheduleId} AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByScheduleId(Long scheduleId);

    @Select("""
            SELECT id, source_type, schedule_id, semester_id, class_id, teacher_id,
                   experiment_course_id, experiment_course_slot_id, experiment_course_instance_id,
                   status, static_code, token_ttl_seconds, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE semester_id = #{semesterId}
              AND class_id = #{classId}
              AND teacher_id = #{teacherId}
              AND source_type = 'CLASS_SCHEDULE'
              AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByKey(Long semesterId, Long classId, Long teacherId);

    @Select("""
            SELECT id, source_type, schedule_id, semester_id, class_id, teacher_id,
                   experiment_course_id, experiment_course_slot_id, experiment_course_instance_id,
                   status, static_code, token_ttl_seconds, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE static_code = #{code}
              AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByStaticCode(String code);

    @Select("""
            SELECT id, source_type, schedule_id, semester_id, class_id, teacher_id,
                   experiment_course_id, experiment_course_slot_id, experiment_course_instance_id,
                   status, static_code, token_ttl_seconds, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE experiment_course_instance_id = #{instanceId}
              AND source_type = 'EXPERIMENT_COURSE'
              AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByExperimentInstanceId(Long instanceId);

    @Select("""
            <script>
            SELECT s.id AS session_id,
                   s.source_type,
                   CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN ec.title ELSE COALESCE(cs.course_name, '未命名课程') END AS course_name,
                   s.class_id,
                   CASE
                     WHEN s.source_type = 'EXPERIMENT_COURSE' THEN COALESCE(NULLIF(TRIM(ecs.name), ''), CONCAT('场次', ecs.id))
                     WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                     ELSE c.name
                   END AS class_display_name,
                   CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN NULL ELSE c.grade END AS grade,
                   s.experiment_course_id,
                   s.experiment_course_slot_id,
                   s.experiment_course_instance_id,
                   COALESCE(exp_room.name, class_room.name) AS lab_room_name,
                   CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN ecsi.lesson_date ELSE cs.lesson_date END AS lesson_date,
                   CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN ecsi.display_name ELSE class_slot.name END AS slot_name,
                   s.started_at,
                   s.ended_at,
                   s.status,
                   COALESCE(rec.checked_in_count, 0) AS checked_in_count,
                   CASE
                     WHEN s.source_type = 'EXPERIMENT_COURSE' THEN COALESCE(snap_roster.total_count, exp_roster.total_count, 0)
                     ELSE COALESCE(class_roster.total_count, 0)
                   END AS total_count
            FROM attendance_session s
            LEFT JOIN course_schedule cs ON cs.id = s.schedule_id AND s.source_type = 'CLASS_SCHEDULE'
            LEFT JOIN org_class c ON c.id = s.class_id AND s.source_type = 'CLASS_SCHEDULE'
            LEFT JOIN lab_room class_room ON class_room.id = cs.lab_room_id
            LEFT JOIN time_slot class_slot ON class_slot.id = cs.slot_id
            LEFT JOIN experiment_course ec ON ec.id = s.experiment_course_id AND s.source_type = 'EXPERIMENT_COURSE'
            LEFT JOIN experiment_course_slot ecs ON ecs.id = s.experiment_course_slot_id AND s.source_type = 'EXPERIMENT_COURSE'
            LEFT JOIN experiment_course_slot_instance ecsi ON ecsi.id = s.experiment_course_instance_id AND s.source_type = 'EXPERIMENT_COURSE'
            LEFT JOIN lab_room exp_room ON exp_room.id = ecsi.lab_room_id
            LEFT JOIN (
                SELECT session_id, COUNT(DISTINCT student_id) AS checked_in_count
                FROM attendance_record
                GROUP BY session_id
            ) rec ON rec.session_id = s.id
            LEFT JOIN (
                SELECT su.class_id, COUNT(*) AS total_count
                FROM sys_user su
                JOIN sys_user_role ur ON ur.user_id = su.id
                JOIN sys_role sr ON sr.id = ur.role_id
                WHERE su.enabled = TRUE
                  AND su.class_id IS NOT NULL
                  AND sr.code = 'ROLE_STUDENT'
                GROUP BY su.class_id
            ) class_roster ON class_roster.class_id = s.class_id
            LEFT JOIN (
                SELECT session_id, COUNT(*) AS total_count
                FROM attendance_session_roster
                GROUP BY session_id
            ) snap_roster ON snap_roster.session_id = s.id
            LEFT JOIN (
                SELECT slot_id, COUNT(*) AS total_count
                FROM experiment_course_enrollment
                WHERE status = 'ENROLLED'
                GROUP BY slot_id
            ) exp_roster ON exp_roster.slot_id = s.experiment_course_slot_id
            WHERE 1 = 1
            <if test="teacherId != null"> AND s.teacher_id = #{teacherId}</if>
            <if test="sourceType != null and sourceType != ''"> AND s.source_type = #{sourceType}</if>
            <if test="grade != null">
              AND (
                s.source_type = 'CLASS_SCHEDULE'
                AND (
                  c.grade = #{grade}
                  OR (c.grade IS NULL AND c.name LIKE CONCAT(#{grade}, '级%'))
                )
              )
            </if>
            <if test="classId != null"> AND s.source_type = 'CLASS_SCHEDULE' AND s.class_id = #{classId}</if>
            <if test="roomKeyword != null and roomKeyword != ''">
              AND COALESCE(exp_room.name, class_room.name) LIKE CONCAT('%', #{roomKeyword}, '%')
            </if>
            <if test="from != null"> AND s.started_at <![CDATA[>=]]> #{from}</if>
            <if test="to != null"> AND s.started_at <![CDATA[<=]]> #{to}</if>
            <if test="status != null and status != ''"> AND s.status = #{status}</if>
            ORDER BY s.started_at DESC, s.id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<TeacherAttendanceSessionListItemVO> findTeacherHistorySessions(
            @Param("teacherId") Long teacherId,
            @Param("sourceType") String sourceType,
            @Param("grade") Integer grade,
            @Param("classId") Long classId,
            @Param("roomKeyword") String roomKeyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") String status,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM attendance_session s
            LEFT JOIN course_schedule cs ON cs.id = s.schedule_id AND s.source_type = 'CLASS_SCHEDULE'
            LEFT JOIN org_class c ON c.id = s.class_id AND s.source_type = 'CLASS_SCHEDULE'
            LEFT JOIN lab_room class_room ON class_room.id = cs.lab_room_id
            LEFT JOIN experiment_course_slot_instance ecsi ON ecsi.id = s.experiment_course_instance_id AND s.source_type = 'EXPERIMENT_COURSE'
            LEFT JOIN lab_room exp_room ON exp_room.id = ecsi.lab_room_id
            WHERE 1 = 1
            <if test="teacherId != null"> AND s.teacher_id = #{teacherId}</if>
            <if test="sourceType != null and sourceType != ''"> AND s.source_type = #{sourceType}</if>
            <if test="grade != null">
              AND (
                s.source_type = 'CLASS_SCHEDULE'
                AND (
                  c.grade = #{grade}
                  OR (c.grade IS NULL AND c.name LIKE CONCAT(#{grade}, '级%'))
                )
              )
            </if>
            <if test="classId != null"> AND s.source_type = 'CLASS_SCHEDULE' AND s.class_id = #{classId}</if>
            <if test="roomKeyword != null and roomKeyword != ''">
              AND COALESCE(exp_room.name, class_room.name) LIKE CONCAT('%', #{roomKeyword}, '%')
            </if>
            <if test="from != null"> AND s.started_at <![CDATA[>=]]> #{from}</if>
            <if test="to != null"> AND s.started_at <![CDATA[<=]]> #{to}</if>
            <if test="status != null and status != ''"> AND s.status = #{status}</if>
            </script>
            """)
    long countTeacherHistorySessions(
            @Param("teacherId") Long teacherId,
            @Param("sourceType") String sourceType,
            @Param("grade") Integer grade,
            @Param("classId") Long classId,
            @Param("roomKeyword") String roomKeyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") String status
    );

    @Select("""
            SELECT s.id AS session_id,
                   s.source_type,
                   CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN ec.title ELSE COALESCE(cs.course_name, '未命名课程') END AS course_name,
                   s.class_id,
                   CASE
                     WHEN s.source_type = 'EXPERIMENT_COURSE' THEN COALESCE(NULLIF(TRIM(ecs.name), ''), CONCAT('场次', ecs.id))
                     WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                     ELSE c.name
                   END AS class_display_name,
                   CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN NULL ELSE c.grade END AS grade,
                   s.experiment_course_id,
                   s.experiment_course_slot_id,
                   s.experiment_course_instance_id,
                   COALESCE(exp_room.name, class_room.name) AS lab_room_name,
                   CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN ecsi.lesson_date ELSE cs.lesson_date END AS lesson_date,
                   CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN ecsi.display_name ELSE class_slot.name END AS slot_name,
                   s.started_at,
                   s.ended_at,
                   s.status,
                   COALESCE(rec.checked_in_count, 0) AS checked_in_count,
                   CASE
                     WHEN s.source_type = 'EXPERIMENT_COURSE' THEN COALESCE(snap_roster.total_count, exp_roster.total_count, 0)
                     ELSE COALESCE(class_roster.total_count, 0)
                   END AS total_count
            FROM attendance_session s
            LEFT JOIN course_schedule cs ON cs.id = s.schedule_id AND s.source_type = 'CLASS_SCHEDULE'
            LEFT JOIN org_class c ON c.id = s.class_id AND s.source_type = 'CLASS_SCHEDULE'
            LEFT JOIN lab_room class_room ON class_room.id = cs.lab_room_id
            LEFT JOIN time_slot class_slot ON class_slot.id = cs.slot_id
            LEFT JOIN experiment_course ec ON ec.id = s.experiment_course_id AND s.source_type = 'EXPERIMENT_COURSE'
            LEFT JOIN experiment_course_slot ecs ON ecs.id = s.experiment_course_slot_id AND s.source_type = 'EXPERIMENT_COURSE'
            LEFT JOIN experiment_course_slot_instance ecsi ON ecsi.id = s.experiment_course_instance_id AND s.source_type = 'EXPERIMENT_COURSE'
            LEFT JOIN lab_room exp_room ON exp_room.id = ecsi.lab_room_id
            LEFT JOIN (
                SELECT session_id, COUNT(DISTINCT student_id) AS checked_in_count
                FROM attendance_record
                GROUP BY session_id
            ) rec ON rec.session_id = s.id
            LEFT JOIN (
                SELECT su.class_id, COUNT(*) AS total_count
                FROM sys_user su
                JOIN sys_user_role ur ON ur.user_id = su.id
                JOIN sys_role sr ON sr.id = ur.role_id
                WHERE su.enabled = TRUE
                  AND su.class_id IS NOT NULL
                  AND sr.code = 'ROLE_STUDENT'
                GROUP BY su.class_id
            ) class_roster ON class_roster.class_id = s.class_id
            LEFT JOIN (
                SELECT session_id, COUNT(*) AS total_count
                FROM attendance_session_roster
                GROUP BY session_id
            ) snap_roster ON snap_roster.session_id = s.id
            LEFT JOIN (
                SELECT slot_id, COUNT(*) AS total_count
                FROM experiment_course_enrollment
                WHERE status = 'ENROLLED'
                GROUP BY slot_id
            ) exp_roster ON exp_roster.slot_id = s.experiment_course_slot_id
            WHERE s.id = #{sessionId}
            LIMIT 1
            """)
    TeacherAttendanceSessionListItemVO findHistorySessionById(@Param("sessionId") Long sessionId);
}
