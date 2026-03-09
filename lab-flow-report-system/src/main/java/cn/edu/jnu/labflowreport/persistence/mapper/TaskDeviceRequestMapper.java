package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceRequestEntity;
import cn.edu.jnu.labflowreport.flow.vo.TaskDeviceRequestVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskDeviceRequestMapper extends BaseMapper<TaskDeviceRequestEntity> {

    @Select("""
            SELECT id,
                   task_id,
                   student_id,
                   device_id,
                   quantity,
                   status,
                   note,
                   approved_by,
                   approved_at,
                   rejected_by,
                   rejected_at,
                   checkout_by,
                   checkout_at,
                   return_by,
                   return_at,
                   created_at
            FROM task_device_request
            WHERE id = #{id}
            FOR UPDATE
            """)
    TaskDeviceRequestEntity findByIdForUpdate(Long id);

    @Select("""
            SELECT COALESCE(SUM(quantity), 0)
            FROM task_device_request
            WHERE task_id = #{taskId}
              AND device_id = #{deviceId}
              AND status IN ('PENDING', 'APPROVED', 'BORROWED')
            """)
    Integer sumReservedByTaskAndDevice(Long taskId, Long deviceId);

    @Select("""
            SELECT COALESCE(SUM(quantity), 0)
            FROM task_device_request
            WHERE device_id = #{deviceId}
              AND status IN ('PENDING', 'APPROVED', 'BORROWED')
            """)
    Integer sumReservedByDevice(Long deviceId);

    @Select("""
            SELECT r.id,
                   r.task_id,
                   r.student_id,
                   u.username AS student_username,
                   u.display_name AS student_display_name,
                   r.device_id,
                   d.code AS device_code,
                   d.name AS device_name,
                   r.quantity,
                   r.status,
                   r.note,
                   r.created_at,
                   r.approved_at,
                   r.checkout_at,
                   r.return_at
            FROM task_device_request r
            JOIN sys_user u ON u.id = r.student_id
            JOIN device d ON d.id = r.device_id
            WHERE r.task_id = #{taskId} AND r.student_id = #{studentId}
            ORDER BY r.created_at DESC, r.id DESC
            """)
    List<TaskDeviceRequestVO> findByTaskAndStudent(Long taskId, Long studentId);

    @Select("""
            SELECT r.id,
                   r.task_id,
                   r.student_id,
                   u.username AS student_username,
                   u.display_name AS student_display_name,
                   r.device_id,
                   d.code AS device_code,
                   d.name AS device_name,
                   r.quantity,
                   r.status,
                   r.note,
                   r.created_at,
                   r.approved_at,
                   r.checkout_at,
                   r.return_at
            FROM task_device_request r
            JOIN sys_user u ON u.id = r.student_id
            JOIN device d ON d.id = r.device_id
            WHERE r.task_id = #{taskId}
            ORDER BY r.created_at DESC, r.id DESC
            """)
    List<TaskDeviceRequestVO> findByTaskId(Long taskId);
}
