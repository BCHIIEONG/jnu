package cn.edu.jnu.labflowreport.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExportRecordEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportReviewEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportSubmissionEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExpTaskMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExportRecordMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportReviewMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportSubmissionMapper;
import cn.edu.jnu.labflowreport.workflow.dto.ReviewCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.SubmissionCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.TaskCreateRequest;
import cn.edu.jnu.labflowreport.workflow.vo.ReviewVO;
import cn.edu.jnu.labflowreport.workflow.vo.ScoreExportRowVO;
import cn.edu.jnu.labflowreport.workflow.vo.SubmissionVO;
import cn.edu.jnu.labflowreport.workflow.vo.TaskVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportWorkflowService {

    private final ExpTaskMapper expTaskMapper;
    private final ReportSubmissionMapper submissionMapper;
    private final ReportReviewMapper reviewMapper;
    private final ExportRecordMapper exportRecordMapper;

    public ReportWorkflowService(
            ExpTaskMapper expTaskMapper,
            ReportSubmissionMapper submissionMapper,
            ReportReviewMapper reviewMapper,
            ExportRecordMapper exportRecordMapper
    ) {
        this.expTaskMapper = expTaskMapper;
        this.submissionMapper = submissionMapper;
        this.reviewMapper = reviewMapper;
        this.exportRecordMapper = exportRecordMapper;
    }

    @Transactional
    public TaskVO createTask(AuthenticatedUser user, TaskCreateRequest request) {
        ExpTaskEntity entity = new ExpTaskEntity();
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setPublisherId(user.userId());
        entity.setDeadlineAt(request.deadlineAt());
        entity.setStatus("OPEN");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        expTaskMapper.insert(entity);
        return getTask(entity.getId());
    }

    public List<TaskVO> listTasks() {
        return expTaskMapper.findTaskList();
    }

    public TaskVO getTask(Long taskId) {
        TaskVO task = expTaskMapper.findTaskById(taskId);
        if (task == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    @Transactional
    public SubmissionVO submitReport(Long taskId, AuthenticatedUser student, SubmissionCreateRequest request) {
        ensureTaskExists(taskId);
        Integer currentVersion = submissionMapper.findMaxVersion(taskId, student.userId());
        int nextVersion = (currentVersion == null ? 0 : currentVersion) + 1;

        ReportSubmissionEntity entity = new ReportSubmissionEntity();
        entity.setTaskId(taskId);
        entity.setStudentId(student.userId());
        entity.setVersionNo(nextVersion);
        entity.setContentMd(request.contentMd());
        entity.setSubmitStatus("SUBMITTED");
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        submissionMapper.insert(entity);
        return submissionMapper.findMySubmissionsByTask(taskId, student.userId()).stream()
                .filter(item -> item.getId().equals(entity.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, "提交记录创建成功但读取失败"));
    }

    public List<SubmissionVO> listMySubmissions(Long taskId, AuthenticatedUser student) {
        ensureTaskExists(taskId);
        return submissionMapper.findMySubmissionsByTask(taskId, student.userId());
    }

    public List<SubmissionVO> listTaskSubmissions(Long taskId) {
        ensureTaskExists(taskId);
        return submissionMapper.findSubmissionsByTask(taskId);
    }

    @Transactional
    public ReviewVO reviewSubmission(Long submissionId, AuthenticatedUser teacher, ReviewCreateRequest request) {
        Long studentId = submissionMapper.findStudentIdBySubmissionId(submissionId);
        if (studentId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "提交记录不存在");
        }

        ReportReviewEntity review = reviewMapper.selectOne(
                new LambdaQueryWrapper<ReportReviewEntity>()
                        .eq(ReportReviewEntity::getSubmissionId, submissionId)
        );
        if (review == null) {
            review = new ReportReviewEntity();
            review.setSubmissionId(submissionId);
            review.setTeacherId(teacher.userId());
            review.setScore(normalizeScore(request.score()));
            review.setComment(request.comment());
            review.setReviewedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());
            reviewMapper.insert(review);
        } else {
            review.setTeacherId(teacher.userId());
            review.setScore(normalizeScore(request.score()));
            review.setComment(request.comment());
            review.setReviewedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());
            reviewMapper.updateById(review);
        }
        return getReview(submissionId, teacher);
    }

    public ReviewVO getReview(Long submissionId, AuthenticatedUser user) {
        ReviewVO review = reviewMapper.findReviewBySubmissionId(submissionId);
        if (review == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "该提交尚未批阅");
        }

        if (user.roleCodes().contains("ROLE_TEACHER") || user.roleCodes().contains("ROLE_ADMIN")) {
            return review;
        }

        Long ownerId = submissionMapper.findStudentIdBySubmissionId(submissionId);
        if (ownerId != null && ownerId.equals(user.userId())) {
            return review;
        }

        throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权查看该批阅结果");
    }

    @Transactional
    public String exportScoresCsv(Long taskId, AuthenticatedUser operator) {
        ensureTaskExists(taskId);
        List<ScoreExportRowVO> rows = submissionMapper.findScoreRowsByTask(taskId);

        ExportRecordEntity record = new ExportRecordEntity();
        record.setOperatorId(operator.userId());
        record.setExportType("TASK_SCORE_CSV");
        record.setConditionJson("{\"taskId\":" + taskId + "}");
        record.setCreatedAt(LocalDateTime.now());
        exportRecordMapper.insert(record);

        StringBuilder csv = new StringBuilder();
        csv.append("studentUsername,studentDisplayName,versionNo,score,comment,submittedAt,reviewedAt\n");
        for (ScoreExportRowVO row : rows) {
            csv.append(csvCell(row.getStudentUsername())).append(",");
            csv.append(csvCell(row.getStudentDisplayName())).append(",");
            csv.append(csvCell(row.getVersionNo())).append(",");
            csv.append(csvCell(row.getScore())).append(",");
            csv.append(csvCell(row.getComment())).append(",");
            csv.append(csvCell(row.getSubmittedAt())).append(",");
            csv.append(csvCell(row.getReviewedAt())).append("\n");
        }
        return csv.toString();
    }

    private void ensureTaskExists(Long taskId) {
        ExpTaskEntity task = expTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务不存在");
        }
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        return score.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}

