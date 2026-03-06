package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.PlagSubmissionBestMatchEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlagSubmissionBestMatchMapper extends BaseMapper<PlagSubmissionBestMatchEntity> {

    @Select("""
            SELECT id, run_id, task_id, submission_id, student_id,
                   best_other_submission_id, best_other_student_id,
                   max_score, evidence_json, skipped_attachments_json, created_at
            FROM plag_submission_best_match
            WHERE run_id = #{runId} AND submission_id = #{submissionId}
            LIMIT 1
            """)
    PlagSubmissionBestMatchEntity findByRunIdAndSubmissionId(Long runId, Long submissionId);
}

