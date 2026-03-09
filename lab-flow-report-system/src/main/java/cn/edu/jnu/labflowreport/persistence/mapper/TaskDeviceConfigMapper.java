package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceConfigEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskDeviceConfigMapper extends BaseMapper<TaskDeviceConfigEntity> {

    @Select("""
            SELECT id, task_id, device_id, max_quantity, created_at
            FROM task_device_config
            WHERE task_id = #{taskId}
            ORDER BY id ASC
            """)
    List<TaskDeviceConfigEntity> findByTaskId(Long taskId);
}
