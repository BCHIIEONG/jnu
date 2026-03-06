package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskTargetClassEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExpTaskTargetClassMapper extends BaseMapper<ExpTaskTargetClassEntity> {

    @Select("""
            SELECT class_id
            FROM exp_task_target_class
            WHERE task_id = #{taskId}
            ORDER BY class_id ASC
            """)
    List<Long> findTargetClassIds(Long taskId);
}

