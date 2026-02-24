package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.ReportSubmissionEntity;
import cn.edu.jnu.labflowreport.workflow.vo.ScoreExportRowVO;
import cn.edu.jnu.labflowreport.workflow.vo.SubmissionVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportSubmissionMapper extends BaseMapper<ReportSubmissionEntity> {

    @Select("""
            SELECT COALESCE(MAX(version_no), 0)
            FROM report_submission
            WHERE task_id = #{taskId} AND student_id = #{studentId}
            """)
    Integer findMaxVersion(Long taskId, Long studentId);

    @Select("""
            SELECT s.id,
                   s.task_id,
                   s.student_id,
                   u.username AS student_username,
                   u.display_name AS student_display_name,
                   s.version_no,
                   s.content_md,
                   s.submit_status,
                   s.submitted_at
            FROM report_submission s
            JOIN sys_user u ON u.id = s.student_id
            WHERE s.task_id = #{taskId} AND s.student_id = #{studentId}
            ORDER BY s.version_no DESC
            """)
    List<SubmissionVO> findMySubmissionsByTask(Long taskId, Long studentId);

    @Select("""
            SELECT s.id,
                   s.task_id,
                   s.student_id,
                   u.username AS student_username,
                   u.display_name AS student_display_name,
                   s.version_no,
                   s.content_md,
                   s.submit_status,
                   s.submitted_at
            FROM report_submission s
            JOIN sys_user u ON u.id = s.student_id
            WHERE s.task_id = #{taskId}
            ORDER BY s.submitted_at DESC
            """)
    List<SubmissionVO> findSubmissionsByTask(Long taskId);

    @Select("""
            SELECT s.id,
                   s.task_id,
                   s.student_id,
                   u.username AS student_username,
                   u.display_name AS student_display_name,
                   s.version_no,
                   s.content_md,
                   s.submit_status,
                   s.submitted_at
            FROM report_submission s
            JOIN sys_user u ON u.id = s.student_id
            WHERE s.id = #{submissionId}
            """)
    SubmissionVO findSubmissionById(Long submissionId);

    @Select("SELECT student_id FROM report_submission WHERE id = #{submissionId}")
    Long findStudentIdBySubmissionId(Long submissionId);

    @Select("""
            SELECT u.username AS student_username,
                   u.display_name AS student_display_name,
                   s.version_no,
                   r.score,
                   r.comment,
                   s.submitted_at,
                   r.reviewed_at
            FROM report_submission s
            JOIN sys_user u ON u.id = s.student_id
            LEFT JOIN report_review r ON r.submission_id = s.id
            WHERE s.task_id = #{taskId}
            ORDER BY u.username ASC, s.version_no DESC
            """)
    List<ScoreExportRowVO> findScoreRowsByTask(Long taskId);
}
