package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskProgressAttachmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskProgressAttachmentMapper extends BaseMapper<TaskProgressAttachmentEntity> {

    @Select("""
            SELECT id, progress_log_id, file_name, content_type, file_size, relative_path, file_sha256, created_at
            FROM task_progress_attachment
            WHERE progress_log_id = #{progressLogId}
            ORDER BY id ASC
            """)
    List<TaskProgressAttachmentEntity> findByProgressLogId(Long progressLogId);
}
