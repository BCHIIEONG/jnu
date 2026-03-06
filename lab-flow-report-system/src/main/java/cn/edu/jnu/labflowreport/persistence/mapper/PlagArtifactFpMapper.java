package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.PlagArtifactFpEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlagArtifactFpMapper extends BaseMapper<PlagArtifactFpEntity> {

    @Select("""
            SELECT id, run_id, task_id, submission_id, student_id, attachment_id,
                   artifact_type, algo, fp64_hex, byte_len, content_type, file_name, created_at
            FROM plag_artifact_fp
            WHERE run_id = #{runId}
            """)
    List<PlagArtifactFpEntity> findByRunId(Long runId);

    @Select("""
            SELECT artifact_type AS artifactType, COUNT(1) AS cnt
            FROM plag_artifact_fp
            WHERE run_id = #{runId} AND submission_id = #{submissionId}
            GROUP BY artifact_type
            """)
    List<ArtifactTypeCountRow> countByRunIdAndSubmissionId(Long runId, Long submissionId);

    class ArtifactTypeCountRow {
        private String artifactType;
        private Long cnt;

        public String getArtifactType() {
            return artifactType;
        }

        public void setArtifactType(String artifactType) {
            this.artifactType = artifactType;
        }

        public Long getCnt() {
            return cnt;
        }

        public void setCnt(Long cnt) {
            this.cnt = cnt;
        }
    }
}
