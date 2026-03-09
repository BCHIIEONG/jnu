package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskAttachmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskAttachmentMapper extends BaseMapper<TaskAttachmentEntity> {

    @Select("""
            SELECT id, task_id, file_name, file_path, file_size, content_type, uploaded_by, uploaded_at, created_at
            FROM task_attachment
            WHERE task_id = #{taskId}
            ORDER BY uploaded_at ASC, id ASC
            """)
    List<TaskAttachmentEntity> findByTaskId(Long taskId);
}
