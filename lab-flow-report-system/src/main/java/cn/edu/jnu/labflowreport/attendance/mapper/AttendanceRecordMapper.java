package cn.edu.jnu.labflowreport.attendance.mapper;

import cn.edu.jnu.labflowreport.attendance.entity.AttendanceRecordEntity;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecordEntity> {

    @Select("SELECT COUNT(1) FROM attendance_record WHERE student_id = #{studentId}")
    long countByStudentId(@Param("studentId") Long studentId);

    @Select("""
            SELECT r.id,
                   r.session_id,
                   r.student_id,
                   u.username AS student_username,
                   u.display_name AS student_display_name,
                   r.method,
                   r.checked_in_at,
                   r.operator_id
            FROM attendance_record r
            JOIN sys_user u ON u.id = r.student_id
            WHERE r.session_id = #{sessionId}
            ORDER BY r.checked_in_at ASC, r.id ASC
            """)
    List<AttendanceRecordVO> findRecordsBySessionId(@Param("sessionId") Long sessionId);
}
