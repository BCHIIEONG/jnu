package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.PlagTaskRunEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlagTaskRunMapper extends BaseMapper<PlagTaskRunEntity> {

    @Select("""
            SELECT id, task_id, status, algo_version, text_threshold, image_threshold,
                   started_at, finished_at, summary_json
            FROM plag_task_run
            WHERE task_id = #{taskId}
            ORDER BY started_at DESC, id DESC
            LIMIT 1
            """)
    PlagTaskRunEntity findLatestByTaskId(Long taskId);
}

