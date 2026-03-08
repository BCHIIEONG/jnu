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
            SELECT id, schedule_id, semester_id, class_id, teacher_id, status, static_code, token_ttl_seconds, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE schedule_id = #{scheduleId} AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByScheduleId(Long scheduleId);

    @Select("""
            SELECT id, schedule_id, semester_id, class_id, teacher_id, status, static_code, token_ttl_seconds, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE semester_id = #{semesterId}
              AND class_id = #{classId}
              AND teacher_id = #{teacherId}
              AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByKey(Long semesterId, Long classId, Long teacherId);

    @Select("""
            SELECT id, schedule_id, semester_id, class_id, teacher_id, status, static_code, token_ttl_seconds, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE static_code = #{code}
              AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByStaticCode(String code);

    @Select("""
            <script>
            SELECT s.id AS session_id,
                   COALESCE(cs.course_name, '未命名课程') AS course_name,
                   s.class_id,
                   CASE WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name) ELSE c.name END AS class_display_name,
                   c.grade,
                   r.name AS lab_room_name,
                   cs.lesson_date,
                   ts.name AS slot_name,
                   s.started_at,
                   s.ended_at,
                   s.status,
                   COALESCE(rec.checked_in_count, 0) AS checked_in_count,
                   COALESCE(roster.total_count, 0) AS total_count
            FROM attendance_session s
            LEFT JOIN course_schedule cs ON cs.id = s.schedule_id
            LEFT JOIN org_class c ON c.id = s.class_id
            LEFT JOIN lab_room r ON r.id = cs.lab_room_id
            LEFT JOIN time_slot ts ON ts.id = cs.slot_id
            LEFT JOIN (
                SELECT session_id, COUNT(DISTINCT student_id) AS checked_in_count
                FROM attendance_record
                GROUP BY session_id
            ) rec ON rec.session_id = s.id
            LEFT JOIN (
                SELECT class_id, COUNT(*) AS total_count
                FROM sys_user
                WHERE enabled = TRUE AND class_id IS NOT NULL
                GROUP BY class_id
            ) roster ON roster.class_id = s.class_id
            WHERE s.teacher_id = #{teacherId}
            <if test="grade != null">
              AND (
                c.grade = #{grade}
                OR (c.grade IS NULL AND c.name LIKE CONCAT(#{grade}, '级%'))
              )
            </if>
            <if test="classId != null"> AND s.class_id = #{classId}</if>
            <if test="roomKeyword != null and roomKeyword != ''">
              AND r.name LIKE CONCAT('%', #{roomKeyword}, '%')
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
            LEFT JOIN course_schedule cs ON cs.id = s.schedule_id
            LEFT JOIN org_class c ON c.id = s.class_id
            LEFT JOIN lab_room r ON r.id = cs.lab_room_id
            WHERE s.teacher_id = #{teacherId}
            <if test="grade != null">
              AND (
                c.grade = #{grade}
                OR (c.grade IS NULL AND c.name LIKE CONCAT(#{grade}, '级%'))
              )
            </if>
            <if test="classId != null"> AND s.class_id = #{classId}</if>
            <if test="roomKeyword != null and roomKeyword != ''">
              AND r.name LIKE CONCAT('%', #{roomKeyword}, '%')
            </if>
            <if test="from != null"> AND s.started_at <![CDATA[>=]]> #{from}</if>
            <if test="to != null"> AND s.started_at <![CDATA[<=]]> #{to}</if>
            <if test="status != null and status != ''"> AND s.status = #{status}</if>
            </script>
            """)
    long countTeacherHistorySessions(
            @Param("teacherId") Long teacherId,
            @Param("grade") Integer grade,
            @Param("classId") Long classId,
            @Param("roomKeyword") String roomKeyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") String status
    );

    @Select("""
            SELECT s.id AS session_id,
                   COALESCE(cs.course_name, '未命名课程') AS course_name,
                   s.class_id,
                   CASE WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name) ELSE c.name END AS class_display_name,
                   c.grade,
                   r.name AS lab_room_name,
                   cs.lesson_date,
                   ts.name AS slot_name,
                   s.started_at,
                   s.ended_at,
                   s.status,
                   COALESCE(rec.checked_in_count, 0) AS checked_in_count,
                   COALESCE(roster.total_count, 0) AS total_count
            FROM attendance_session s
            LEFT JOIN course_schedule cs ON cs.id = s.schedule_id
            LEFT JOIN org_class c ON c.id = s.class_id
            LEFT JOIN lab_room r ON r.id = cs.lab_room_id
            LEFT JOIN time_slot ts ON ts.id = cs.slot_id
            LEFT JOIN (
                SELECT session_id, COUNT(DISTINCT student_id) AS checked_in_count
                FROM attendance_record
                GROUP BY session_id
            ) rec ON rec.session_id = s.id
            LEFT JOIN (
                SELECT class_id, COUNT(*) AS total_count
                FROM sys_user
                WHERE enabled = TRUE AND class_id IS NOT NULL
                GROUP BY class_id
            ) roster ON roster.class_id = s.class_id
            WHERE s.id = #{sessionId}
            LIMIT 1
            """)
    TeacherAttendanceSessionListItemVO findHistorySessionById(@Param("sessionId") Long sessionId);
}
