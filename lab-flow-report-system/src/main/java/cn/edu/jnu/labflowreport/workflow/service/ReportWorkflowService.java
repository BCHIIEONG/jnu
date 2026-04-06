package cn.edu.jnu.labflowreport.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.export.ExcelExportService;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.util.ClassDisplayUtils;
import cn.edu.jnu.labflowreport.common.util.HashUtils;
import cn.edu.jnu.labflowreport.elective.service.ExperimentCourseService;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskTargetClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExportRecordEntity;
import cn.edu.jnu.labflowreport.persistence.entity.PlagTaskRunEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportAttachmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportReviewEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportSubmissionEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskAttachmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskCompletionEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceConfigEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceRequestEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskProgressAttachmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskProgressLogEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExpTaskMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExpTaskTargetClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExportRecordMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.PlagArtifactFpMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.PlagSubmissionBestMatchMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.PlagTaskRunMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportAttachmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportReviewMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportSubmissionMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskAttachmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskCompletionMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDeviceConfigMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDeviceRequestMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskProgressAttachmentMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final TaskProgressAttachmentMapper taskProgressAttachmentMapper;
    private final TaskCompletionMapper taskCompletionMapper;
    private final TaskDeviceConfigMapper taskDeviceConfigMapper;
    private final TaskDeviceRequestMapper taskDeviceRequestMapper;
    private final PlagTaskRunMapper plagTaskRunMapper;
    private final PlagArtifactFpMapper plagArtifactFpMapper;
    private final PlagSubmissionBestMatchMapper plagSubmissionBestMatchMapper;
    private final ExperimentCourseService experimentCourseService;
    private final OrgClassMapper orgClassMapper;
    private final ExcelExportService excelExportService;

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
            TaskProgressAttachmentMapper taskProgressAttachmentMapper,
            TaskCompletionMapper taskCompletionMapper,
            TaskDeviceConfigMapper taskDeviceConfigMapper,
            TaskDeviceRequestMapper taskDeviceRequestMapper,
            PlagTaskRunMapper plagTaskRunMapper,
            PlagArtifactFpMapper plagArtifactFpMapper,
            PlagSubmissionBestMatchMapper plagSubmissionBestMatchMapper,
            ExperimentCourseService experimentCourseService,
            OrgClassMapper orgClassMapper,
            ExcelExportService excelExportService
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
        this.taskProgressAttachmentMapper = taskProgressAttachmentMapper;
        this.taskCompletionMapper = taskCompletionMapper;
        this.taskDeviceConfigMapper = taskDeviceConfigMapper;
        this.taskDeviceRequestMapper = taskDeviceRequestMapper;
        this.plagTaskRunMapper = plagTaskRunMapper;
        this.plagArtifactFpMapper = plagArtifactFpMapper;
        this.plagSubmissionBestMatchMapper = plagSubmissionBestMatchMapper;
        this.experimentCourseService = experimentCourseService;
        this.orgClassMapper = orgClassMapper;
        this.excelExportService = excelExportService;
    }

    @Transactional
    public TaskVO createTask(AuthenticatedUser user, TaskCreateRequest request) {
        ExpTaskEntity entity = new ExpTaskEntity();
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setPublisherId(user.userId());
        entity.setExperimentCourseId(request.experimentCourseId());
        entity.setDeadlineAt(request.deadlineAt());
        entity.setStatus("OPEN");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        validateExperimentCourseBinding(user, request.experimentCourseId());
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
        getTaskEntityOrThrow(taskId);

        List<TaskAttachmentEntity> taskAttachments = taskAttachmentMapper.findByTaskId(taskId);
        List<ReportSubmissionEntity> submissions = submissionMapper.selectList(new LambdaQueryWrapper<ReportSubmissionEntity>()
                .eq(ReportSubmissionEntity::getTaskId, taskId));
        List<Long> submissionIds = submissions.stream()
                .map(ReportSubmissionEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        List<ReportAttachmentEntity> reportAttachments = new ArrayList<>();
        if (!submissionIds.isEmpty()) {
            reportAttachments = attachmentMapper.selectList(new LambdaQueryWrapper<ReportAttachmentEntity>()
                    .in(ReportAttachmentEntity::getSubmissionId, submissionIds));
        }

        List<TaskProgressLogEntity> progressLogs = taskProgressLogMapper.selectList(new LambdaQueryWrapper<TaskProgressLogEntity>()
                .eq(TaskProgressLogEntity::getTaskId, taskId));
        List<Long> progressLogIds = progressLogs.stream()
                .map(TaskProgressLogEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        List<TaskProgressAttachmentEntity> progressAttachments = new ArrayList<>();
        if (!progressLogIds.isEmpty()) {
            progressAttachments = taskProgressAttachmentMapper.selectList(new LambdaQueryWrapper<TaskProgressAttachmentEntity>()
                    .in(TaskProgressAttachmentEntity::getProgressLogId, progressLogIds));
        }

        if (!submissionIds.isEmpty()) {
            reviewMapper.delete(new LambdaQueryWrapper<ReportReviewEntity>()
                    .in(ReportReviewEntity::getSubmissionId, submissionIds));
            attachmentMapper.delete(new LambdaQueryWrapper<ReportAttachmentEntity>()
                    .in(ReportAttachmentEntity::getSubmissionId, submissionIds));
        }

        if (!progressLogIds.isEmpty()) {
            taskProgressAttachmentMapper.delete(new LambdaQueryWrapper<TaskProgressAttachmentEntity>()
                    .in(TaskProgressAttachmentEntity::getProgressLogId, progressLogIds));
        }

        plagSubmissionBestMatchMapper.delete(new LambdaQueryWrapper<cn.edu.jnu.labflowreport.persistence.entity.PlagSubmissionBestMatchEntity>()
                .eq(cn.edu.jnu.labflowreport.persistence.entity.PlagSubmissionBestMatchEntity::getTaskId, taskId));
        plagArtifactFpMapper.delete(new LambdaQueryWrapper<cn.edu.jnu.labflowreport.persistence.entity.PlagArtifactFpEntity>()
                .eq(cn.edu.jnu.labflowreport.persistence.entity.PlagArtifactFpEntity::getTaskId, taskId));
        plagTaskRunMapper.delete(new LambdaQueryWrapper<PlagTaskRunEntity>()
                .eq(PlagTaskRunEntity::getTaskId, taskId));
        taskCompletionMapper.delete(new LambdaQueryWrapper<TaskCompletionEntity>()
                .eq(TaskCompletionEntity::getTaskId, taskId));
        taskDeviceRequestMapper.delete(new LambdaQueryWrapper<TaskDeviceRequestEntity>()
                .eq(TaskDeviceRequestEntity::getTaskId, taskId));
        taskDeviceConfigMapper.delete(new LambdaQueryWrapper<TaskDeviceConfigEntity>()
                .eq(TaskDeviceConfigEntity::getTaskId, taskId));
        taskProgressLogMapper.delete(new LambdaQueryWrapper<TaskProgressLogEntity>()
                .eq(TaskProgressLogEntity::getTaskId, taskId));
        taskAttachmentMapper.delete(new LambdaQueryWrapper<TaskAttachmentEntity>()
                .eq(TaskAttachmentEntity::getTaskId, taskId));
        taskTargetClassMapper.delete(new LambdaQueryWrapper<ExpTaskTargetClassEntity>()
                .eq(ExpTaskTargetClassEntity::getTaskId, taskId));
        submissionMapper.delete(new LambdaQueryWrapper<ReportSubmissionEntity>()
                .eq(ReportSubmissionEntity::getTaskId, taskId));
        expTaskMapper.deleteById(taskId);

        deleteStoredFiles(taskAttachments.stream().map(TaskAttachmentEntity::getFilePath).toList());
        deleteStoredFiles(reportAttachments.stream().map(ReportAttachmentEntity::getFilePath).toList());
        deleteStoredFiles(progressAttachments.stream().map(TaskProgressAttachmentEntity::getRelativePath).toList());
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
        ensureTeacherOrAdminCanAccessSubmission(submissionId, teacher);

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

        if (user.roleCodes().contains("ROLE_ADMIN")) {
            return review;
        }
        if (user.roleCodes().contains("ROLE_TEACHER")) {
            ensureTeacherOrAdminCanAccessSubmission(submissionId, user);
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

    @Transactional
    public byte[] exportScoresExcel(Long taskId, AuthenticatedUser operator) {
        ensureTeacherOrAdminCanManageTask(taskId, operator);
        TaskVO task = getTask(taskId);
        List<SubmissionVO> submissions = submissionMapper.findSubmissionsByTask(taskId);
        List<ScoreExportRowVO> scoreRows = submissionMapper.findScoreRowsByTask(taskId);
        List<SysUserEntity> students = sysUserMapper.findStudentsForTask(taskId);
        Map<Long, String> classDisplayMap = loadClassDisplayMap(students);
        Map<Long, List<SubmissionVO>> submissionsByStudent = submissions.stream()
                .collect(Collectors.groupingBy(SubmissionVO::getStudentId));

        var summaryRows = scoreRows.stream()
                .map(item -> row(
                        task.getTitle(),
                        item.getStudentDisplayName(),
                        item.getStudentUsername(),
                        item.getVersionNo(),
                        item.getScore(),
                        item.getComment(),
                        item.getSubmittedAt(),
                        item.getReviewedAt()))
                .toList();

        var submissionRows = submissions.stream()
                .map(item -> row(
                        task.getTitle(),
                        item.getStudentDisplayName(),
                        item.getStudentUsername(),
                        classDisplayMap.get(item.getStudentId()),
                        item.getVersionNo(),
                        item.getSubmitStatus(),
                        item.getSubmittedAt()))
                .toList();

        var unsubmittedRows = students.stream()
                .filter(student -> !submissionsByStudent.containsKey(student.getId()))
                .map(student -> row(
                        task.getTitle(),
                        student.getDisplayName(),
                        student.getUsername(),
                        classDisplayMap.get(student.getId()),
                        "是"))
                .toList();

        var reviewRows = scoreRows.stream()
                .filter(row -> row.getReviewedAt() != null || row.getScore() != null || (row.getComment() != null && !row.getComment().isBlank()))
                .map(item -> row(
                        task.getTitle(),
                        item.getStudentDisplayName(),
                        item.getStudentUsername(),
                        classDisplayMap.get(findStudentIdByUsername(students, item.getStudentUsername())),
                        item.getVersionNo(),
                        item.getScore(),
                        item.getComment(),
                        item.getReviewedAt()))
                .toList();

        var completionRows = students.stream()
                .map(student -> {
                    TaskCompletionEntity completion = taskCompletionMapper.findByTaskAndStudent(taskId, student.getId());
                    return row(
                            task.getTitle(),
                            student.getDisplayName(),
                            student.getUsername(),
                            classDisplayMap.get(student.getId()),
                            completion == null ? "NONE" : completion.getStatus(),
                            completion == null ? null : completion.getCompletionSource(),
                            completion == null ? null : completion.getRequestedAt(),
                            completion == null ? null : completion.getConfirmedAt(),
                            completion == null ? null : resolveUserDisplayName(completion.getConfirmedBy()));
                })
                .toList();

        ExportRecordEntity record = new ExportRecordEntity();
        record.setOperatorId(operator.userId());
        record.setExportType("TASK_SCORE_EXCEL");
        record.setConditionJson("{\"taskId\":" + taskId + "}");
        record.setCreatedAt(LocalDateTime.now());
        exportRecordMapper.insert(record);

        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec(
                        "筛选条件",
                        List.of("字段", "值"),
                        List.of(
                                row("任务ID", taskId),
                                row("任务标题", task.getTitle()),
                                row("导出时间", LocalDateTime.now()),
                                row("操作者", operator.username())
                        )
                ),
                new ExcelExportService.SheetSpec(
                        "成绩汇总",
                        List.of("任务", "学生姓名", "用户名", "版本号", "分数", "批语", "提交时间", "批阅时间"),
                        summaryRows
                ),
                new ExcelExportService.SheetSpec(
                        "提交明细",
                        List.of("任务", "学生姓名", "用户名", "班级", "版本号", "提交状态", "提交时间"),
                        submissionRows
                ),
                new ExcelExportService.SheetSpec(
                        "未提交名单",
                        List.of("任务", "学生姓名", "用户名", "班级", "任务可见"),
                        unsubmittedRows
                ),
                new ExcelExportService.SheetSpec(
                        "批阅明细",
                        List.of("任务", "学生姓名", "用户名", "班级", "版本号", "分数", "批语", "批阅时间"),
                        reviewRows
                ),
                new ExcelExportService.SheetSpec(
                        "完成登记明细",
                        List.of("任务", "学生姓名", "用户名", "班级", "完成状态", "完成来源", "申请时间", "确认时间", "确认教师"),
                        completionRows
                )
        ));
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
        SysUserEntity me = sysUserMapper.selectById(studentId);
        if (me == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户不存在或已被删除");
        }
        Integer accessible = expTaskMapper.countStudentAccessibleTask(taskId, studentId);
        if (accessible == null || accessible <= 0) {
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

    void ensureTeacherOrAdminCanManageTask(Long taskId, AuthenticatedUser actor) {
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

    private void ensureTeacherOrAdminCanAccessSubmission(Long submissionId, AuthenticatedUser actor) {
        Long taskId = submissionMapper.findTaskIdBySubmissionId(submissionId);
        if (taskId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "提交记录不存在");
        }
        ensureTeacherOrAdminCanManageTask(taskId, actor);
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

    private Map<Long, String> loadClassDisplayMap(List<SysUserEntity> students) {
        Set<Long> classIds = students.stream().map(SysUserEntity::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return orgClassMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(
                        cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity::getId,
                        item -> ClassDisplayUtils.effectiveDisplayName(item.getGrade(), item.getName())
                ));
    }

    private Long findStudentIdByUsername(List<SysUserEntity> students, String username) {
        return students.stream()
                .filter(item -> Objects.equals(item.getUsername(), username))
                .map(SysUserEntity::getId)
                .findFirst()
                .orElse(null);
    }

    private List<?> row(Object... values) {
        List<Object> row = new ArrayList<>();
        for (Object value : values) {
            row.add(value);
        }
        return row;
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUserEntity user = sysUserMapper.selectById(userId);
        return user == null ? null : user.getDisplayName();
    }

    private List<TaskAttachmentVO> listTaskAttachmentVos(Long taskId) {
        return taskAttachmentMapper.findByTaskId(taskId).stream()
                .map(this::toTaskAttachmentVo)
                .toList();
    }

    private void validateExperimentCourseBinding(AuthenticatedUser actor, Long experimentCourseId) {
        if (experimentCourseId == null) {
            return;
        }
        experimentCourseService.listTeacherCourses(actor).stream()
                .filter(course -> Objects.equals(course.getId(), experimentCourseId))
                .filter(course -> "OPEN".equalsIgnoreCase(course.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "只能绑定自己创建且已开放的实验课程"));
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

    private void deleteStoredFiles(List<String> relativePaths) {
        for (String relativePath : relativePaths) {
            if (relativePath == null || relativePath.isBlank()) {
                continue;
            }
            storageService.delete(relativePath);
        }
    }

    public record DownloadData(String filename, String contentType, byte[] bytes) {
    }
}
