package cn.edu.jnu.labflowreport.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.util.HashUtils;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskTargetClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExportRecordEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportAttachmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportReviewEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportSubmissionEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskAttachmentEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExpTaskMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExpTaskTargetClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExportRecordMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.PlagTaskRunMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportAttachmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportReviewMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportSubmissionMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskAttachmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskCompletionMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDeviceConfigMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDeviceRequestMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskProgressLogMapper;
import cn.edu.jnu.labflowreport.storage.FileStorageService;
import cn.edu.jnu.labflowreport.workflow.dto.ReviewCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.SubmissionCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.TaskCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.TaskTitleUpdateRequest;
import cn.edu.jnu.labflowreport.workflow.vo.ReviewVO;
import cn.edu.jnu.labflowreport.workflow.vo.ScoreExportRowVO;
import cn.edu.jnu.labflowreport.workflow.vo.SubmissionVO;
import cn.edu.jnu.labflowreport.workflow.vo.TaskAttachmentVO;
import cn.edu.jnu.labflowreport.workflow.vo.TaskVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportWorkflowService {

    private final ExpTaskMapper expTaskMapper;
    private final ReportSubmissionMapper submissionMapper;
    private final ReportAttachmentMapper attachmentMapper;
    private final FileStorageService storageService;
    private final ReportReviewMapper reviewMapper;
    private final ExportRecordMapper exportRecordMapper;
    private final ExpTaskTargetClassMapper taskTargetClassMapper;
    private final SysUserMapper sysUserMapper;
    private final TaskAttachmentMapper taskAttachmentMapper;
    private final TaskProgressLogMapper taskProgressLogMapper;
    private final TaskCompletionMapper taskCompletionMapper;
    private final TaskDeviceConfigMapper taskDeviceConfigMapper;
    private final TaskDeviceRequestMapper taskDeviceRequestMapper;
    private final PlagTaskRunMapper plagTaskRunMapper;

    public ReportWorkflowService(
            ExpTaskMapper expTaskMapper,
            ReportSubmissionMapper submissionMapper,
            ReportAttachmentMapper attachmentMapper,
            FileStorageService storageService,
            ReportReviewMapper reviewMapper,
            ExportRecordMapper exportRecordMapper,
            ExpTaskTargetClassMapper taskTargetClassMapper,
            SysUserMapper sysUserMapper,
            TaskAttachmentMapper taskAttachmentMapper,
            TaskProgressLogMapper taskProgressLogMapper,
            TaskCompletionMapper taskCompletionMapper,
            TaskDeviceConfigMapper taskDeviceConfigMapper,
            TaskDeviceRequestMapper taskDeviceRequestMapper,
            PlagTaskRunMapper plagTaskRunMapper
    ) {
        this.expTaskMapper = expTaskMapper;
        this.submissionMapper = submissionMapper;
        this.attachmentMapper = attachmentMapper;
        this.storageService = storageService;
        this.reviewMapper = reviewMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.taskTargetClassMapper = taskTargetClassMapper;
        this.sysUserMapper = sysUserMapper;
        this.taskAttachmentMapper = taskAttachmentMapper;
        this.taskProgressLogMapper = taskProgressLogMapper;
        this.taskCompletionMapper = taskCompletionMapper;
        this.taskDeviceConfigMapper = taskDeviceConfigMapper;
        this.taskDeviceRequestMapper = taskDeviceRequestMapper;
        this.plagTaskRunMapper = plagTaskRunMapper;
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

        if (request.classIds() != null && !request.classIds().isEmpty()) {
            Set<Long> uniq = request.classIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            for (Long classId : uniq) {
                if (classId == null) continue;
                ExpTaskTargetClassEntity tc = new ExpTaskTargetClassEntity();
                tc.setTaskId(entity.getId());
                tc.setClassId(classId);
                tc.setCreatedAt(LocalDateTime.now());
                taskTargetClassMapper.insert(tc);
            }
        }
        return getTask(entity.getId());
    }

    public List<TaskVO> listTasks(AuthenticatedUser user) {
        if (user == null || user.roleCodes() == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
        }
        if (user.roleCodes().contains("ROLE_ADMIN")) {
            return expTaskMapper.findTaskList();
        }
        if (user.roleCodes().contains("ROLE_TEACHER")) {
            return expTaskMapper.findTaskListForTeacher(user.userId());
        }
        return expTaskMapper.findTaskListForStudent(user.userId());
    }

    public TaskVO getTask(Long taskId) {
        TaskVO task = expTaskMapper.findTaskById(taskId);
        if (task == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务不存在");
        }
        task.setAttachments(listTaskAttachmentVos(taskId));
        return task;
    }

    public List<TaskAttachmentVO> listTaskAttachments(Long taskId, AuthenticatedUser user) {
        getTaskForUser(taskId, user);
        return listTaskAttachmentVos(taskId);
    }

    @Transactional
    public List<TaskAttachmentVO> uploadTaskAttachments(Long taskId, AuthenticatedUser actor, MultipartFile[] files) {
        ensureTeacherOrAdminCanManageTask(taskId, actor);
        List<MultipartFile> incoming = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    incoming.add(file);
                }
            }
        }
        if (incoming.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "请选择要上传的附件");
        }

        for (MultipartFile file : incoming) {
            FileStorageService.SaveResult saved = storageService.saveTaskAttachmentWithSha256(taskId, file);
            TaskAttachmentEntity entity = new TaskAttachmentEntity();
            entity.setTaskId(taskId);
            entity.setFileName(Objects.toString(file.getOriginalFilename(), "attachment"));
            entity.setFilePath(saved.relativePath());
            entity.setFileSize(file.getSize());
            entity.setContentType(file.getContentType());
            entity.setUploadedBy(actor.userId());
            entity.setUploadedAt(LocalDateTime.now());
            entity.setCreatedAt(LocalDateTime.now());
            taskAttachmentMapper.insert(entity);
        }
        return listTaskAttachmentVos(taskId);
    }

    @Transactional
    public void deleteTaskAttachment(Long taskId, Long attachmentId, AuthenticatedUser actor) {
        ensureTeacherOrAdminCanManageTask(taskId, actor);
        TaskAttachmentEntity entity = taskAttachmentMapper.selectById(attachmentId);
        if (entity == null || !taskId.equals(entity.getTaskId())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务附件不存在");
        }
        taskAttachmentMapper.deleteById(attachmentId);
        storageService.delete(entity.getFilePath());
    }

    public DownloadData downloadTaskAttachment(Long attachmentId, AuthenticatedUser user) {
        TaskAttachmentEntity entity = taskAttachmentMapper.selectById(attachmentId);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务附件不存在");
        }
        getTaskForUser(entity.getTaskId(), user);
        byte[] bytes = storageService.readBytes(entity.getFilePath());
        return new DownloadData(entity.getFileName(), entity.getContentType(), bytes);
    }

    @Transactional
    public void deleteTask(Long taskId, AuthenticatedUser actor) {
        ensureTeacherOrAdminCanManageTask(taskId, actor);
        ensureTaskCanBeDeleted(taskId);

        List<TaskAttachmentEntity> attachments = taskAttachmentMapper.findByTaskId(taskId);
        for (TaskAttachmentEntity attachment : attachments) {
            taskAttachmentMapper.deleteById(attachment.getId());
            storageService.delete(attachment.getFilePath());
        }

        taskDeviceConfigMapper.delete(new LambdaQueryWrapper<cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceConfigEntity>()
                .eq(cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceConfigEntity::getTaskId, taskId));
        taskTargetClassMapper.delete(new LambdaQueryWrapper<ExpTaskTargetClassEntity>()
                .eq(ExpTaskTargetClassEntity::getTaskId, taskId));
        expTaskMapper.deleteById(taskId);
    }

    @Transactional
    public TaskVO updateTaskTitle(Long taskId, AuthenticatedUser actor, TaskTitleUpdateRequest request) {
        ensureTeacherOrAdminCanManageTask(taskId, actor);
        ExpTaskEntity entity = expTaskMapper.selectById(taskId);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务不存在");
        }
        entity.setTitle(request.title().trim());
        entity.setUpdatedAt(LocalDateTime.now());
        expTaskMapper.updateById(entity);
        return getTask(taskId);
    }

    public TaskVO getTaskForUser(Long taskId, AuthenticatedUser user) {
        TaskVO task = getTask(taskId);
        if (user == null || user.roleCodes() == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
        }
        if (user.roleCodes().contains("ROLE_ADMIN") || user.roleCodes().contains("ROLE_TEACHER")) {
            // Teachers can only access tasks they published.
            if (user.roleCodes().contains("ROLE_TEACHER") && !user.roleCodes().contains("ROLE_ADMIN")) {
                if (task.getPublisherId() != null && !task.getPublisherId().equals(user.userId())) {
                    throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权访问该任务");
                }
            }
            return task;
        }
        ensureStudentCanAccessTask(taskId, user.userId());
        return task;
    }

    @Transactional
    public SubmissionVO submitReport(Long taskId, AuthenticatedUser student, SubmissionCreateRequest request) {
        ensureStudentCanSubmitTask(taskId, student);

        String normalized = normalizeForHash(request.contentMd());
        String sha = HashUtils.sha256Hex(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        ReportSubmissionEntity prev = findLatestSubmissionEntity(taskId, student.userId());
        if (prev != null) {
            String prevSha = prev.getContentSha256();
            if (prevSha == null || prevSha.isBlank()) {
                prevSha = HashUtils.sha256Hex(normalizeForHash(prev.getContentMd()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            if (sha.equalsIgnoreCase(prevSha)) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "正文内容没有变化，禁止重复提交");
            }
        }

        Integer currentVersion = submissionMapper.findMaxVersion(taskId, student.userId());
        int nextVersion = (currentVersion == null ? 0 : currentVersion) + 1;

        ReportSubmissionEntity entity = new ReportSubmissionEntity();
        entity.setTaskId(taskId);
        entity.setStudentId(student.userId());
        entity.setVersionNo(nextVersion);
        entity.setContentMd(request.contentMd());
        entity.setContentSha256(sha);
        entity.setSubmitStatus("SUBMITTED");
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        submissionMapper.insert(entity);
        return submissionMapper.findMySubmissionsByTask(taskId, student.userId()).stream()
                .filter(item -> item.getId().equals(entity.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, "提交记录创建成功但读取失败"));
    }

    @Transactional
    public SubmissionVO submitReportMultipart(
            Long taskId,
            AuthenticatedUser student,
            String contentMd,
            boolean confirmEmptyContent,
            MultipartFile[] files
    ) {
        ensureStudentCanSubmitTask(taskId, student);

        String content = Objects.toString(contentMd, "");
        String normalized = normalizeForHash(content);
        boolean hasText = !normalized.isBlank();

        List<MultipartFile> incoming = new ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) incoming.add(f);
            }
        }

        if (!hasText && incoming.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "正文为空且未上传附件");
        }

        ReportSubmissionEntity prev = findLatestSubmissionEntity(taskId, student.userId());
        String prevNormalized = "";
        String prevSha = "";
        Set<String> prevAttSha = Set.of();
        if (prev != null) {
            prevNormalized = normalizeForHash(prev.getContentMd());
            prevSha = prev.getContentSha256();
            if (prevSha == null || prevSha.isBlank()) {
                prevSha = HashUtils.sha256Hex(prevNormalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            if (!prevNormalized.isBlank() && !hasText && !confirmEmptyContent) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "本次提交没有正文内容，请确认后再提交");
            }
            prevAttSha = attachmentMapper.findBySubmissionId(prev.getId()).stream()
                    .map(this::ensureAttachmentSha256)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toSet());
        }

        String sha = HashUtils.sha256Hex(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        boolean contentChanged = (prev == null) || !sha.equalsIgnoreCase(prevSha);

        List<FileWithSha> newFiles = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (MultipartFile f : incoming) {
            String fileSha = hashMultipartFileSha256(f);
            if (fileSha.isBlank()) continue;
            if (seen.contains(fileSha)) continue;
            seen.add(fileSha);
            if (prevAttSha.contains(fileSha)) {
                continue; // duplicate of previous version
            }
            newFiles.add(new FileWithSha(f, fileSha));
        }

        boolean hasNewAttachment = !newFiles.isEmpty();
        if (!contentChanged && !hasNewAttachment) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "没有新增内容，禁止提交");
        }

        Integer currentVersion = submissionMapper.findMaxVersion(taskId, student.userId());
        int nextVersion = (currentVersion == null ? 0 : currentVersion) + 1;

        ReportSubmissionEntity entity = new ReportSubmissionEntity();
        entity.setTaskId(taskId);
        entity.setStudentId(student.userId());
        entity.setVersionNo(nextVersion);
        entity.setContentMd(content);
        entity.setContentSha256(sha);
        entity.setSubmitStatus("SUBMITTED");
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        submissionMapper.insert(entity);

        for (FileWithSha f : newFiles) {
            FileStorageService.SaveResult saved = storageService.saveReportAttachmentWithSha256(entity.getId(), f.file());

            ReportAttachmentEntity att = new ReportAttachmentEntity();
            att.setSubmissionId(entity.getId());
            att.setFileName(Objects.toString(f.file().getOriginalFilename(), "attachment"));
            att.setFilePath(saved.relativePath());
            att.setFileSize(f.file().getSize());
            att.setContentType(f.file().getContentType());
            att.setFileSha256(saved.sha256Hex());
            att.setUploadedAt(LocalDateTime.now());
            attachmentMapper.insert(att);
        }

        return submissionMapper.findMySubmissionsByTask(taskId, student.userId()).stream()
                .filter(item -> item.getId().equals(entity.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, "提交记录创建成功但读取失败"));
    }

    public List<SubmissionVO> listMySubmissions(Long taskId, AuthenticatedUser student) {
        ensureStudentCanAccessTask(taskId, student.userId());
        return submissionMapper.findMySubmissionsByTask(taskId, student.userId());
    }

    public List<SubmissionVO> listTaskSubmissions(Long taskId) {
        ensureTaskExists(taskId);
        return submissionMapper.findSubmissionsByTask(taskId);
    }

    public List<SubmissionVO> listTaskSubmissions(Long taskId, AuthenticatedUser actor) {
        ensureTeacherOrAdminCanManageTask(taskId, actor);
        return submissionMapper.findSubmissionsByTask(taskId);
    }

    @Transactional
    public TaskVO updateTaskStatus(Long taskId, AuthenticatedUser actor, cn.edu.jnu.labflowreport.workflow.dto.TaskStatusUpdateRequest request) {
        ensureTeacherOrAdminCanManageTask(taskId, actor);
        String status = request == null ? null : request.status();
        if (!"OPEN".equalsIgnoreCase(status) && !"CLOSED".equalsIgnoreCase(status)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "status 只能是 OPEN 或 CLOSED");
        }
        ExpTaskEntity task = getTaskEntityOrThrow(taskId);
        task.setStatus(status.toUpperCase(java.util.Locale.ROOT));
        task.setUpdatedAt(LocalDateTime.now());
        expTaskMapper.updateById(task);
        return getTask(taskId);
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
        ensureTeacherOrAdminCanManageTask(taskId, operator);
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

    private ExpTaskEntity getTaskEntityOrThrow(Long taskId) {
        ExpTaskEntity task = expTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private void ensureStudentCanAccessTask(Long taskId, Long studentId) {
        if (taskId == null || studentId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "参数错误");
        }
        List<Long> targets = taskTargetClassMapper.findTargetClassIds(taskId);
        if (targets.isEmpty()) {
            return; // global task
        }
        SysUserEntity me = sysUserMapper.selectById(studentId);
        if (me == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户不存在或已被删除");
        }
        if (me.getClassId() == null || !targets.contains(me.getClassId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "不属于该任务的发布班级");
        }
    }

    private void ensureStudentCanSubmitTask(Long taskId, AuthenticatedUser student) {
        if (student == null || student.roleCodes() == null || !student.roleCodes().contains("ROLE_STUDENT")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "只有学生可以提交报告");
        }
        ExpTaskEntity task = getTaskEntityOrThrow(taskId);
        if (!"OPEN".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "任务已关闭，无法提交");
        }
        if (task.getDeadlineAt() != null && LocalDateTime.now().isAfter(task.getDeadlineAt())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "已超过截止时间，无法提交");
        }
        ensureStudentCanAccessTask(taskId, student.userId());
    }

    private void ensureTeacherOrAdminCanManageTask(Long taskId, AuthenticatedUser actor) {
        if (actor == null || actor.roleCodes() == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
        }
        ExpTaskEntity task = getTaskEntityOrThrow(taskId);
        if (actor.roleCodes().contains("ROLE_ADMIN")) {
            return;
        }
        if (!actor.roleCodes().contains("ROLE_TEACHER")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权限访问该资源");
        }
        if (task.getPublisherId() == null || !task.getPublisherId().equals(actor.userId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "只能管理自己发布的任务");
        }
    }

    private ReportSubmissionEntity findLatestSubmissionEntity(Long taskId, Long studentId) {
        if (taskId == null || studentId == null) return null;
        return submissionMapper.selectOne(
                new LambdaQueryWrapper<ReportSubmissionEntity>()
                        .eq(ReportSubmissionEntity::getTaskId, taskId)
                        .eq(ReportSubmissionEntity::getStudentId, studentId)
                        .orderByDesc(ReportSubmissionEntity::getVersionNo)
                        .last("LIMIT 1")
        );
    }

    private String normalizeForHash(String content) {
        String s = Objects.toString(content, "");
        s = s.replace("\r\n", "\n");
        return s.trim();
    }

    private String hashMultipartFileSha256(MultipartFile file) {
        try (var in = file.getInputStream()) {
            return HashUtils.sha256Hex(in);
        } catch (Exception e) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "附件读取失败");
        }
    }

    private String ensureAttachmentSha256(ReportAttachmentEntity att) {
        if (att == null) return "";
        String sha = att.getFileSha256();
        if (sha != null && !sha.isBlank()) {
            return sha;
        }
        try {
            byte[] bytes = storageService.readBytes(att.getFilePath());
            sha = HashUtils.sha256Hex(bytes);
            attachmentMapper.updateFileSha256(att.getId(), sha);
            return sha;
        } catch (Exception e) {
            return "";
        }
    }

    private record FileWithSha(MultipartFile file, String sha256) {
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

    private List<TaskAttachmentVO> listTaskAttachmentVos(Long taskId) {
        return taskAttachmentMapper.findByTaskId(taskId).stream()
                .map(this::toTaskAttachmentVo)
                .toList();
    }

    private void ensureTaskCanBeDeleted(Long taskId) {
        long submissionCount = submissionMapper.selectCount(new LambdaQueryWrapper<ReportSubmissionEntity>()
                .eq(ReportSubmissionEntity::getTaskId, taskId));
        long progressCount = taskProgressLogMapper.selectCount(new LambdaQueryWrapper<cn.edu.jnu.labflowreport.persistence.entity.TaskProgressLogEntity>()
                .eq(cn.edu.jnu.labflowreport.persistence.entity.TaskProgressLogEntity::getTaskId, taskId));
        long completionCount = taskCompletionMapper.selectCount(new LambdaQueryWrapper<cn.edu.jnu.labflowreport.persistence.entity.TaskCompletionEntity>()
                .eq(cn.edu.jnu.labflowreport.persistence.entity.TaskCompletionEntity::getTaskId, taskId));
        long deviceRequestCount = taskDeviceRequestMapper.selectCount(new LambdaQueryWrapper<cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceRequestEntity>()
                .eq(cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceRequestEntity::getTaskId, taskId));
        long plagRunCount = plagTaskRunMapper.selectCount(new LambdaQueryWrapper<cn.edu.jnu.labflowreport.persistence.entity.PlagTaskRunEntity>()
                .eq(cn.edu.jnu.labflowreport.persistence.entity.PlagTaskRunEntity::getTaskId, taskId));

        if (submissionCount > 0 || progressCount > 0 || completionCount > 0 || deviceRequestCount > 0 || plagRunCount > 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "任务已有业务记录，不能删除；如仅需停止使用，请关闭任务");
        }
    }

    private TaskAttachmentVO toTaskAttachmentVo(TaskAttachmentEntity entity) {
        TaskAttachmentVO vo = new TaskAttachmentVO();
        vo.setId(entity.getId());
        vo.setTaskId(entity.getTaskId());
        vo.setFileName(entity.getFileName());
        vo.setFileSize(entity.getFileSize());
        vo.setContentType(entity.getContentType());
        vo.setUploadedBy(entity.getUploadedBy());
        vo.setUploadedAt(entity.getUploadedAt());
        return vo;
    }

    public record DownloadData(String filename, String contentType, byte[] bytes) {
    }
}
