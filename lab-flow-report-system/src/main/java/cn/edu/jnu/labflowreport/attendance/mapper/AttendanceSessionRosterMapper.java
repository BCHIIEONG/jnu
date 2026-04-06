package cn.edu.jnu.labflowreport.attendance.mapper;

import cn.edu.jnu.labflowreport.attendance.entity.AttendanceSessionRosterEntity;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceSessionRosterRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceSessionRosterMapper extends BaseMapper<AttendanceSessionRosterEntity> {

    @Select("""
            SELECT sr.student_id,
                   su.username AS student_username,
                   su.display_name AS student_display_name,
                   sr.class_id
            FROM attendance_session_roster sr
            JOIN sys_user su ON su.id = sr.student_id
            WHERE sr.session_id = #{sessionId}
            ORDER BY su.username ASC, sr.id ASC
            """)
    List<AttendanceSessionRosterRow> findRowsBySessionId(@Param("sessionId") Long sessionId);

    @Select("SELECT COUNT(1) FROM attendance_session_roster WHERE session_id = #{sessionId}")
    long countBySessionId(@Param("sessionId") Long sessionId);

    @Select("SELECT COUNT(1) FROM attendance_session_roster WHERE session_id = #{sessionId} AND student_id = #{studentId}")
    Integer countBySessionAndStudent(@Param("sessionId") Long sessionId, @Param("studentId") Long studentId);
}
