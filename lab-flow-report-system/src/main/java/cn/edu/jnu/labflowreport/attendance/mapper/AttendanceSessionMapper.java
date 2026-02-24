package cn.edu.jnu.labflowreport.attendance.mapper;

import cn.edu.jnu.labflowreport.attendance.entity.AttendanceSessionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceSessionMapper extends BaseMapper<AttendanceSessionEntity> {

    @Select("""
            SELECT id, schedule_id, semester_id, class_id, teacher_id, status, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE schedule_id = #{scheduleId} AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByScheduleId(Long scheduleId);

    @Select("""
            SELECT id, schedule_id, semester_id, class_id, teacher_id, status, started_at, ended_at, created_at, updated_at
            FROM attendance_session
            WHERE semester_id = #{semesterId}
              AND class_id = #{classId}
              AND teacher_id = #{teacherId}
              AND status = 'OPEN'
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    AttendanceSessionEntity findOpenByKey(Long semesterId, Long classId, Long teacherId);
}

