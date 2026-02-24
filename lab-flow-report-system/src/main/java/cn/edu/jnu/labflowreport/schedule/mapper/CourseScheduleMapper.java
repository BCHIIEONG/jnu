package cn.edu.jnu.labflowreport.schedule.mapper;

import cn.edu.jnu.labflowreport.schedule.entity.CourseScheduleEntity;
import cn.edu.jnu.labflowreport.schedule.vo.CourseScheduleVO;
import cn.edu.jnu.labflowreport.schedule.vo.TeacherWeekScheduleItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseScheduleEntity> {

    @Select("""
            <script>
            SELECT cs.id,
                   cs.semester_id,
                   cs.class_id,
                   c.name AS class_name,
                   cs.teacher_id,
                   t.display_name AS teacher_display_name,
                   cs.lab_room_id,
                   r.name AS lab_room_name,
                   cs.lesson_date,
                   cs.slot_id,
                   ts.code AS slot_code,
                   ts.name AS slot_name,
                   ts.start_time AS slot_start_time,
                   ts.end_time AS slot_end_time,
                   cs.course_name
            FROM course_schedule cs
            JOIN org_class c ON c.id = cs.class_id
            JOIN sys_user t ON t.id = cs.teacher_id
            LEFT JOIN lab_room r ON r.id = cs.lab_room_id
            JOIN time_slot ts ON ts.id = cs.slot_id
            WHERE 1=1
            <if test="semesterId != null"> AND cs.semester_id = #{semesterId}</if>
            <if test="from != null"> AND cs.lesson_date &gt;= #{from}</if>
            <if test="to != null"> AND cs.lesson_date &lt;= #{to}</if>
            <if test="teacherId != null"> AND cs.teacher_id = #{teacherId}</if>
            <if test="classId != null"> AND cs.class_id = #{classId}</if>
            ORDER BY cs.lesson_date ASC, ts.start_time ASC, cs.id ASC
            </script>
            """)
    List<CourseScheduleVO> findSchedules(
            @Param("semesterId") Long semesterId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("teacherId") Long teacherId,
            @Param("classId") Long classId
    );

    @Select("""
            SELECT cs.id,
                   cs.semester_id,
                   cs.class_id,
                   c.name AS class_name,
                   cs.teacher_id,
                   t.display_name AS teacher_display_name,
                   cs.lab_room_id,
                   r.name AS lab_room_name,
                   cs.lesson_date,
                   cs.slot_id,
                   ts.code AS slot_code,
                   ts.name AS slot_name,
                   ts.start_time AS slot_start_time,
                   ts.end_time AS slot_end_time,
                   cs.course_name
            FROM course_schedule cs
            JOIN org_class c ON c.id = cs.class_id
            JOIN sys_user t ON t.id = cs.teacher_id
            LEFT JOIN lab_room r ON r.id = cs.lab_room_id
            JOIN time_slot ts ON ts.id = cs.slot_id
            WHERE cs.teacher_id = #{teacherId}
              AND cs.semester_id = #{semesterId}
              AND cs.lesson_date >= #{from}
              AND cs.lesson_date <= #{to}
            ORDER BY cs.lesson_date ASC, ts.start_time ASC, cs.id ASC
            """)
    List<TeacherWeekScheduleItemVO> findTeacherWeek(
            @Param("teacherId") Long teacherId,
            @Param("semesterId") Long semesterId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
