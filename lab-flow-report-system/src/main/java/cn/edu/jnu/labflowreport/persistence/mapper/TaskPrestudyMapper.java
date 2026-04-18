package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskPrestudyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskPrestudyMapper extends BaseMapper<TaskPrestudyEntity> {

    @Select("""
            SELECT id, task_id, title, description, version, published_at, updated_at
            FROM task_prestudy
            WHERE task_id = #{taskId}
            """)
    TaskPrestudyEntity findByTaskId(Long taskId);
}
