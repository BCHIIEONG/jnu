package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.ReportReviewEntity;
import cn.edu.jnu.labflowreport.workflow.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportReviewMapper extends BaseMapper<ReportReviewEntity> {

    @Select("""
            SELECT r.id,
                   r.submission_id,
                   r.teacher_id,
                   t.username AS teacher_username,
                   t.display_name AS teacher_display_name,
                   r.score,
                   r.comment,
                   r.reviewed_at
            FROM report_review r
            JOIN sys_user t ON t.id = r.teacher_id
            WHERE r.submission_id = #{submissionId}
            """)
    ReviewVO findReviewBySubmissionId(Long submissionId);
}

