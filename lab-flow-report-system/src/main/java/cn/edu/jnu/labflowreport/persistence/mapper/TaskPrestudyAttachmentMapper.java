package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskPrestudyAttachmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskPrestudyAttachmentMapper extends BaseMapper<TaskPrestudyAttachmentEntity> {

    @Select("""
            SELECT id, prestudy_id, file_name, file_path, file_size, content_type, uploaded_by, uploaded_at, created_at
            FROM task_prestudy_attachment
            WHERE prestudy_id = #{prestudyId}
            ORDER BY uploaded_at ASC, id ASC
            """)
    List<TaskPrestudyAttachmentEntity> findByPrestudyId(Long prestudyId);
}
