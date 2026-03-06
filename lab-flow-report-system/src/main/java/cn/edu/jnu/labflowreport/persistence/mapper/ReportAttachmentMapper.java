package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.persistence.entity.ReportAttachmentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface ReportAttachmentMapper extends BaseMapper<ReportAttachmentEntity> {

    @Select("""
            SELECT id, submission_id, file_name, file_path, file_size, content_type, file_sha256, uploaded_at
            FROM report_attachment
            WHERE submission_id = #{submissionId}
            ORDER BY uploaded_at ASC, id ASC
            """)
    List<ReportAttachmentEntity> findBySubmissionId(Long submissionId);

    @Update("""
            UPDATE report_attachment
            SET file_sha256 = #{sha256}
            WHERE id = #{id}
            """)
    int updateFileSha256(Long id, String sha256);
}
