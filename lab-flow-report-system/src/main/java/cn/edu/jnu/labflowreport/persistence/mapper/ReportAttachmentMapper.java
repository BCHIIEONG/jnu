package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.persistence.entity.ReportAttachmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface ReportAttachmentMapper extends BaseMapper<ReportAttachmentEntity> {

    @Select("""
            SELECT id, submission_id, file_name, file_path, file_size, content_type, uploaded_at
            FROM report_attachment
            WHERE submission_id = #{submissionId}
            ORDER BY uploaded_at ASC, id ASC
            """)
    List<ReportAttachmentEntity> findBySubmissionId(Long submissionId);
}

