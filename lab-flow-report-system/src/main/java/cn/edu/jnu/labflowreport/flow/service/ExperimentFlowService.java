package cn.edu.jnu.labflowreport.flow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.export.ExcelExportService;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.util.ClassDisplayUtils;
import cn.edu.jnu.labflowreport.common.util.HashUtils;
import cn.edu.jnu.labflowreport.flow.dto.TaskDeviceConfigItemRequest;
import cn.edu.jnu.labflowreport.flow.dto.TaskDeviceRequestCreateRequest;
import cn.edu.jnu.labflowreport.flow.vo.ProgressAttachmentVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskCompletionVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskDeviceConfigVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskDeviceRequestVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskProgressVO;
import cn.edu.jnu.labflowreport.flow.vo.TeacherTaskProgressDetailVO;
import cn.edu.jnu.labflowreport.flow.vo.TeacherTaskProgressStudentVO;
import cn.edu.jnu.labflowreport.persistence.entity.DeviceEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskTargetClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskCompletionEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceConfigEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskDeviceRequestEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskProgressAttachmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.TaskProgressLogEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.DeviceMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExpTaskMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExpTaskTargetClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskCompletionMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDeviceConfigMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskDeviceRequestMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskProgressAttachmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.TaskProgressLogMapper;
import cn.edu.jnu.labflowreport.storage.FileStorageService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExperimentFlowService {

    private static final String COMPLETION_NONE = "NONE";
    private static final String COMPLETION_PENDING = "PENDING_CONFIRM";
    private static final String COMPLETION_CONFIRMED = "CONFIRMED";
    private static final String COMPLETION_SOURCE_STUDENT_REQUEST = "STUDENT_REQUEST";
    private static final String COMPLETION_SOURCE_TEACHER_DIRECT = "TEACHER_DIRECT";
    private static final Set<String> DEVICE_RESERVED_STATUSES = Set.of("PENDING", "APPROVED", "BORROWED");

    private final ExpTaskMapper expTaskMapper;
    private final ExpTaskTargetClassMapper expTaskTargetClassMapper;
    private final SysUserMapper sysUserMapper;
    private final OrgClassMapper orgClassMapper;
    private final DeviceMapper deviceMapper;
    private final TaskProgressLogMapper taskProgressLogMapper;
    private final TaskProgressAttachmentMapper taskProgressAttachmentMapper;
    private final TaskCompletionMapper taskCompletionMapper;
    private final TaskDeviceConfigMapper taskDeviceConfigMapper;
    private final TaskDeviceRequestMapper taskDeviceRequestMapper;
    private final FileStorageService fileStorageService;
    private final ExcelExportService excelExportService;

    public ExperimentFlowService(
            ExpTaskMapper expTaskMapper,
            ExpTaskTargetClassMapper expTaskTargetClassMapper,
            SysUserMapper sysUserMapper,
            OrgClassMapper orgClassMapper,
            DeviceMapper deviceMapper,
            TaskProgressLogMapper taskProgressLogMapper,
            TaskProgressAttachmentMapper taskProgressAttachmentMapper,
            TaskCompletionMapper taskCompletionMapper,
            TaskDeviceConfigMapper taskDeviceConfigMapper,
            TaskDeviceRequestMapper taskDeviceRequestMapper,
            FileStorageService fileStorageService,
            ExcelExportService excelExportService
    ) {
        this.expTaskMapper = expTaskMapper;
        this.expTaskTargetClassMapper = expTaskTargetClassMapper;
        this.sysUserMapper = sysUserMapper;
        this.orgClassMapper = orgClassMapper;
        this.deviceMapper = deviceMapper;
        this.taskProgressLogMapper = taskProgressLogMapper;
        this.taskProgressAttachmentMapper = taskProgressAttachmentMapper;
        this.taskCompletionMapper = taskCompletionMapper;
        this.taskDeviceConfigMapper = taskDeviceConfigMapper;
        this.taskDeviceRequestMapper = taskDeviceRequestMapper;
        this.fileStorageService = fileStorageService;
        this.excelExportService = excelExportService;
    }

    public List<TaskProgressVO> listMyProgress(Long taskId, AuthenticatedUser student) {
        ensureStudentCanAccessTask(taskId, student);
        return toProgressVos(taskId, student.userId());
    }

    @Transactional
    public TaskProgressVO addProgress(Long taskId, AuthenticatedUser student, String content, MultipartFile[] files) {
        ensureStudentCanWriteTask(taskId, student);

        String normalizedContent = Objects.toString(content, "").trim();
        List<MultipartFile> incomingFiles = normalizeFiles(files);
        if (normalizedContent.isBlank() && incomingFiles.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "步骤内容和附件不能同时为空");
        }

        Integer max = taskProgressLogMapper.findMaxStepNo(taskId, student.userId());
        int nextStep = (max == null ? 0 : max) + 1;

        TaskProgressLogEntity entity = new TaskProgressLogEntity();
        entity.setTaskId(taskId);
        entity.setStudentId(student.userId());
        entity.setStepNo(nextStep);
        entity.setContent(normalizedContent);
        entity.setCreatedAt(LocalDateTime.now());
        taskProgressLogMapper.insert(entity);

        for (MultipartFile file : incomingFiles) {
            FileStorageService.SaveResult saved = fileStorageService.saveTaskProgressAttachmentWithSha256(entity.getId(), file);
            TaskProgressAttachmentEntity att = new TaskProgressAttachmentEntity();
            att.setProgressLogId(entity.getId());
            att.setFileName(Objects.toString(file.getOriginalFilename(), "attachment"));
            att.setContentType(file.getContentType());
            att.setFileSize(file.getSize());
            att.setRelativePath(saved.relativePath());
            att.setFileSha256(saved.sha256Hex());
            att.setCreatedAt(LocalDateTime.now());
            taskProgressAttachmentMapper.insert(att);
        }

        return toProgressVos(taskId, student.userId()).stream()
                .filter(v -> Objects.equals(v.id(), entity.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "步骤创建成功但读取失败"));
    }

    public TaskCompletionVO getMyCompletion(Long taskId, AuthenticatedUser student) {
        ensureStudentCanAccessTask(taskId, student);
        return toCompletionVo(taskId, student.userId());
    }

    @Transactional
    public TaskCompletionVO requestCompletion(Long taskId, AuthenticatedUser student) {
        ensureStudentCanWriteTask(taskId, student);
        int count = nvl(taskProgressLogMapper.countByTaskAndStudent(taskId, student.userId()));
        if (count <= 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "请先至少提交一步实验步骤再申请完成登记");
        }

        TaskCompletionEntity existing = taskCompletionMapper.findByTaskAndStudent(taskId, student.userId());
        if (existing != null) {
            if (COMPLETION_CONFIRMED.equals(existing.getStatus())) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "该任务已确认完成");
            }
            if (COMPLETION_PENDING.equals(existing.getStatus())) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "已提交完成登记，等待教师确认");
            }
        }

        TaskCompletionEntity entity = new TaskCompletionEntity();
        entity.setTaskId(taskId);
        entity.setStudentId(student.userId());
        entity.setStatus(COMPLETION_PENDING);
        entity.setCompletionSource(COMPLETION_SOURCE_STUDENT_REQUEST);
        entity.setRequestedAt(LocalDateTime.now());
        taskCompletionMapper.insert(entity);
        return toCompletionVo(taskId, student.userId());
    }

    public List<TeacherTaskProgressStudentVO> listTeacherTaskProgress(Long taskId, AuthenticatedUser teacher, String q) {
        ensureTeacherCanManageTask(taskId, teacher);
        List<SysUserEntity> students = sysUserMapper.findStudentsForTask(taskId);
        Map<Long, String> classNames = loadClassDisplayNames(students);
        List<TeacherTaskProgressStudentVO> rows = new ArrayList<>();
        for (SysUserEntity student : students) {
            TaskCompletionEntity completion = taskCompletionMapper.findByTaskAndStudent(taskId, student.getId());
            rows.add(new TeacherTaskProgressStudentVO(
                    student.getId(),
                    student.getUsername(),
                    student.getDisplayName(),
                    classNames.get(student.getClassId()),
                    nvl(taskProgressLogMapper.countByTaskAndStudent(taskId, student.getId())),
                    completion == null ? COMPLETION_NONE : completion.getStatus(),
                    completion == null ? null : completion.getCompletionSource(),
                    taskProgressLogMapper.findLatestCreatedAt(taskId, student.getId()),
                    completion == null ? null : completion.getRequestedAt(),
                    completion == null ? null : completion.getConfirmedAt()
            ));
        }

        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return rows.stream()
                .filter(row -> keyword.isBlank()
                        || lower(row.studentUsername()).contains(keyword)
                        || lower(row.studentDisplayName()).contains(keyword))
                .sorted(progressStudentComparator())
                .toList();
    }

    public TeacherTaskProgressDetailVO getTeacherTaskProgressDetail(Long taskId, Long studentId, AuthenticatedUser teacher) {
        ensureTeacherCanManageTask(taskId, teacher);
        SysUserEntity student = requireStudentUser(studentId);
        TaskCompletionEntity completion = taskCompletionMapper.findByTaskAndStudent(taskId, studentId);
        return new TeacherTaskProgressDetailVO(
                taskId,
                studentId,
                student.getUsername(),
                student.getDisplayName(),
                loadClassDisplayName(student.getClassId()),
                completion == null ? COMPLETION_NONE : completion.getStatus(),
                completion == null ? null : completion.getCompletionSource(),
                completion == null ? null : completion.getRequestedAt(),
                completion == null ? null : completion.getConfirmedAt(),
                completion == null ? null : resolveUserDisplayName(completion.getConfirmedBy()),
                toProgressVos(taskId, studentId)
        );
    }

    public byte[] exportTaskProgressCompletionExcel(Long taskId, AuthenticatedUser teacher) {
        ensureTeacherCanManageTask(taskId, teacher);
        ExpTaskEntity task = requireTask(taskId);
        List<List<?>> unfinishedRows = new ArrayList<>();
        List<List<?>> confirmedRows = new ArrayList<>();
        for (TeacherTaskProgressStudentVO student : listTeacherTaskProgress(taskId, teacher, null)) {
            if (COMPLETION_CONFIRMED.equals(student.completionStatus())) {
                confirmedRows.add(row(
                        student.studentDisplayName(),
                        student.studentUsername(),
                        student.classDisplayName(),
                        student.progressCount(),
                        completionSourceText(student.completionSource()),
                        student.latestUpdatedAt(),
                        student.confirmedAt()
                ));
            } else {
                unfinishedRows.add(row(
                        student.studentDisplayName(),
                        student.studentUsername(),
                        student.classDisplayName(),
                        student.progressCount(),
                        completionStatusText(student.completionStatus()),
                        student.latestUpdatedAt(),
                        student.requestedAt()
                ));
            }
        }
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec(
                        "未确认完成名单",
                        List.of("学生姓名", "用户名", "班级", "进度条数", "登记状态", "最近更新时间", "申请登记时间"),
                        unfinishedRows
                ),
                new ExcelExportService.SheetSpec(
                        "已确认完成名单",
                        List.of("学生姓名", "用户名", "班级", "进度条数", "完成来源", "最近更新时间", "确认时间"),
                        confirmedRows
                )
        ));
    }

    @Transactional
    public TaskCompletionVO confirmCompletion(Long taskId, Long studentId, AuthenticatedUser teacher) {
        ensureTeacherCanManageTask(taskId, teacher);
        TaskCompletionEntity completion = taskCompletionMapper.findByTaskAndStudent(taskId, studentId);
        if (completion == null || !COMPLETION_PENDING.equals(completion.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前没有待确认的完成登记");
        }
        taskCompletionMapper.update(null, new LambdaUpdateWrapper<TaskCompletionEntity>()
                .eq(TaskCompletionEntity::getId, completion.getId())
                .set(TaskCompletionEntity::getStatus, COMPLETION_CONFIRMED)
                .set(TaskCompletionEntity::getCompletionSource, normalizeCompletionSource(completion.getCompletionSource(), false))
                .set(TaskCompletionEntity::getConfirmedAt, LocalDateTime.now())
                .set(TaskCompletionEntity::getConfirmedBy, teacher.userId()));
        return toCompletionVo(taskId, studentId);
    }

    @Transactional
    public TaskCompletionVO directConfirmCompletion(Long taskId, Long studentId, AuthenticatedUser teacher) {
        ensureTeacherCanManageTask(taskId, teacher);
        requireStudentUser(studentId);
        TaskCompletionEntity completion = taskCompletionMapper.findByTaskAndStudent(taskId, studentId);
        LocalDateTime now = LocalDateTime.now();
        if (completion == null) {
            TaskCompletionEntity entity = new TaskCompletionEntity();
            entity.setTaskId(taskId);
            entity.setStudentId(studentId);
            entity.setStatus(COMPLETION_CONFIRMED);
            entity.setCompletionSource(COMPLETION_SOURCE_TEACHER_DIRECT);
            entity.setRequestedAt(null);
            entity.setConfirmedAt(now);
            entity.setConfirmedBy(teacher.userId());
            taskCompletionMapper.insert(entity);
            return toCompletionVo(taskId, studentId);
        }
        if (COMPLETION_CONFIRMED.equals(completion.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "该任务已登记完成");
        }
        taskCompletionMapper.update(null, new LambdaUpdateWrapper<TaskCompletionEntity>()
                .eq(TaskCompletionEntity::getId, completion.getId())
                .set(TaskCompletionEntity::getStatus, COMPLETION_CONFIRMED)
                .set(TaskCompletionEntity::getCompletionSource, normalizeCompletionSource(completion.getCompletionSource(), true))
                .set(TaskCompletionEntity::getConfirmedAt, now)
                .set(TaskCompletionEntity::getConfirmedBy, teacher.userId()));
        return toCompletionVo(taskId, studentId);
    }

    public List<TaskDeviceConfigVO> listTaskDevicesForTeacher(Long taskId, AuthenticatedUser teacher) {
        ensureTeacherCanManageTask(taskId, teacher);
        return buildTaskDeviceConfigs(taskId, false);
    }

    public List<TaskDeviceConfigVO> listTaskDevicesForStudent(Long taskId, AuthenticatedUser student) {
        ensureStudentCanAccessTask(taskId, student);
        return buildTaskDeviceConfigs(taskId, true);
    }

    @Transactional
    public List<TaskDeviceConfigVO> updateTaskDevices(Long taskId, AuthenticatedUser teacher, List<TaskDeviceConfigItemRequest> request) {
        ensureTeacherCanManageTask(taskId, teacher);
        List<TaskDeviceConfigItemRequest> items = request == null ? List.of() : request;
        Map<Long, TaskDeviceConfigItemRequest> dedup = new HashMap<>();
        for (TaskDeviceConfigItemRequest item : items) {
            if (item == null || item.deviceId() == null) continue;
            dedup.put(item.deviceId(), item);
        }

        taskDeviceConfigMapper.delete(new LambdaQueryWrapper<TaskDeviceConfigEntity>().eq(TaskDeviceConfigEntity::getTaskId, taskId));
        for (TaskDeviceConfigItemRequest item : dedup.values()) {
            if (item.maxQuantity() == null || item.maxQuantity() <= 0) continue;
            DeviceEntity device = deviceMapper.selectById(item.deviceId());
            if (device == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "设备不存在");
            }
            TaskDeviceConfigEntity entity = new TaskDeviceConfigEntity();
            entity.setTaskId(taskId);
            entity.setDeviceId(item.deviceId());
            entity.setMaxQuantity(item.maxQuantity());
            entity.setCreatedAt(LocalDateTime.now());
            taskDeviceConfigMapper.insert(entity);
        }
        return buildTaskDeviceConfigs(taskId, false);
    }

    public List<TaskDeviceRequestVO> listMyDeviceRequests(Long taskId, AuthenticatedUser student) {
        ensureStudentCanAccessTask(taskId, student);
        return taskDeviceRequestMapper.findByTaskAndStudent(taskId, student.userId());
    }

    @Transactional
    public TaskDeviceRequestVO createDeviceRequest(Long taskId, AuthenticatedUser student, TaskDeviceRequestCreateRequest request) {
        ensureStudentCanWriteTask(taskId, student);
        validateDeviceRequestCapacityLocked(taskId, request.deviceId(), request.quantity(), null);

        TaskDeviceRequestEntity entity = new TaskDeviceRequestEntity();
        entity.setTaskId(taskId);
        entity.setStudentId(student.userId());
        entity.setDeviceId(request.deviceId());
        entity.setQuantity(request.quantity());
        entity.setStatus("PENDING");
        entity.setNote(request.note());
        entity.setCreatedAt(LocalDateTime.now());
        taskDeviceRequestMapper.insert(entity);
        return requireDeviceRequestVo(entity.getId(), student.userId(), false);
    }

    public List<TaskDeviceRequestVO> listTeacherDeviceRequests(Long taskId, AuthenticatedUser teacher, String status, String q) {
        ensureTeacherCanManageTask(taskId, teacher);
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String expectedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return taskDeviceRequestMapper.findByTaskId(taskId).stream()
                .filter(item -> expectedStatus.isBlank() || expectedStatus.equalsIgnoreCase(item.status()))
                .filter(item -> keyword.isBlank()
                        || lower(item.studentUsername()).contains(keyword)
                        || lower(item.studentDisplayName()).contains(keyword)
                        || lower(item.deviceName()).contains(keyword)
                        || lower(item.deviceCode()).contains(keyword))
                .sorted(Comparator.comparing(TaskDeviceRequestVO::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public TaskDeviceRequestVO approveDeviceRequest(Long requestId, AuthenticatedUser teacher) {
        TaskDeviceRequestEntity entity = requireDeviceRequestEntityForUpdate(requestId);
        ensureTeacherCanManageTask(entity.getTaskId(), teacher);
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "只有待审批申请可以通过");
        }
        validateDeviceRequestCapacityLocked(entity.getTaskId(), entity.getDeviceId(), entity.getQuantity(), entity.getId());
        taskDeviceRequestMapper.update(null, new LambdaUpdateWrapper<TaskDeviceRequestEntity>()
                .eq(TaskDeviceRequestEntity::getId, requestId)
                .set(TaskDeviceRequestEntity::getStatus, "APPROVED")
                .set(TaskDeviceRequestEntity::getApprovedBy, teacher.userId())
                .set(TaskDeviceRequestEntity::getApprovedAt, LocalDateTime.now()));
        return requireDeviceRequestVo(requestId, teacher.userId(), true);
    }

    @Transactional
    public TaskDeviceRequestVO rejectDeviceRequest(Long requestId, AuthenticatedUser teacher) {
        TaskDeviceRequestEntity entity = requireDeviceRequestEntityForUpdate(requestId);
        ensureTeacherCanManageTask(entity.getTaskId(), teacher);
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "只有待审批申请可以驳回");
        }
        taskDeviceRequestMapper.update(null, new LambdaUpdateWrapper<TaskDeviceRequestEntity>()
                .eq(TaskDeviceRequestEntity::getId, requestId)
                .set(TaskDeviceRequestEntity::getStatus, "REJECTED")
                .set(TaskDeviceRequestEntity::getRejectedBy, teacher.userId())
                .set(TaskDeviceRequestEntity::getRejectedAt, LocalDateTime.now()));
        return requireDeviceRequestVo(requestId, teacher.userId(), true);
    }

    @Transactional
    public TaskDeviceRequestVO checkoutDeviceRequest(Long requestId, AuthenticatedUser teacher) {
        TaskDeviceRequestEntity entity = requireDeviceRequestEntityForUpdate(requestId);
        ensureTeacherCanManageTask(entity.getTaskId(), teacher);
        if (!"APPROVED".equals(entity.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "只有已通过申请可以登记借出");
        }
        validateDeviceRequestCapacityLocked(entity.getTaskId(), entity.getDeviceId(), entity.getQuantity(), entity.getId());
        taskDeviceRequestMapper.update(null, new LambdaUpdateWrapper<TaskDeviceRequestEntity>()
                .eq(TaskDeviceRequestEntity::getId, requestId)
                .set(TaskDeviceRequestEntity::getStatus, "BORROWED")
                .set(TaskDeviceRequestEntity::getCheckoutBy, teacher.userId())
                .set(TaskDeviceRequestEntity::getCheckoutAt, LocalDateTime.now()));
        return requireDeviceRequestVo(requestId, teacher.userId(), true);
    }

    @Transactional
    public TaskDeviceRequestVO returnDeviceRequest(Long requestId, AuthenticatedUser teacher) {
        TaskDeviceRequestEntity entity = requireDeviceRequestEntityForUpdate(requestId);
        ensureTeacherCanManageTask(entity.getTaskId(), teacher);
        if (!"BORROWED".equals(entity.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "只有已借出申请可以登记归还");
        }
        taskDeviceRequestMapper.update(null, new LambdaUpdateWrapper<TaskDeviceRequestEntity>()
                .eq(TaskDeviceRequestEntity::getId, requestId)
                .set(TaskDeviceRequestEntity::getStatus, "RETURNED")
                .set(TaskDeviceRequestEntity::getReturnBy, teacher.userId())
                .set(TaskDeviceRequestEntity::getReturnAt, LocalDateTime.now()));
        return requireDeviceRequestVo(requestId, teacher.userId(), true);
    }

    public String exportTaskDeviceRequestsCsv(Long taskId, AuthenticatedUser teacher) {
        List<TaskDeviceRequestVO> rows = listTeacherDeviceRequests(taskId, teacher, null, null);
        StringBuilder csv = new StringBuilder();
        csv.append("taskId,deviceCode,deviceName,studentUsername,studentDisplayName,quantity,status,createdAt,approvedAt,checkoutAt,returnAt\n");
        for (TaskDeviceRequestVO row : rows) {
            csv.append(cell(taskId)).append(",");
            csv.append(cell(row.deviceCode())).append(",");
            csv.append(cell(row.deviceName())).append(",");
            csv.append(cell(row.studentUsername())).append(",");
            csv.append(cell(row.studentDisplayName())).append(",");
            csv.append(cell(row.quantity())).append(",");
            csv.append(cell(row.status())).append(",");
            csv.append(cell(row.createdAt())).append(",");
            csv.append(cell(row.approvedAt())).append(",");
            csv.append(cell(row.checkoutAt())).append(",");
            csv.append(cell(row.returnAt())).append("\n");
        }
        return csv.toString();
    }

    public byte[] exportTaskDeviceRequestsExcel(Long taskId, AuthenticatedUser teacher) {
        ensureTeacherCanManageTask(taskId, teacher);
        ExpTaskEntity task = requireTask(taskId);
        List<TeacherTaskProgressStudentVO> progressStudents = listTeacherTaskProgress(taskId, teacher, null);
        List<List<?>> progressRows = new ArrayList<>();
        List<List<?>> completionRows = new ArrayList<>();
        for (TeacherTaskProgressStudentVO student : progressStudents) {
            TeacherTaskProgressDetailVO detail = getTeacherTaskProgressDetail(taskId, student.studentId(), teacher);
            completionRows.add(row(
                    task.getTitle(),
                    student.studentDisplayName(),
                    student.studentUsername(),
                    student.classDisplayName(),
                    detail.completionStatus(),
                    detail.completionSource(),
                    detail.requestedAt(),
                    detail.confirmedAt(),
                    detail.confirmedByDisplayName()
            ));
            for (TaskProgressVO log : detail.logs()) {
                String attachmentNames = log.attachments().stream()
                        .map(ProgressAttachmentVO::fileName)
                        .collect(Collectors.joining(" | "));
                progressRows.add(row(
                        task.getTitle(),
                        student.studentDisplayName(),
                        student.studentUsername(),
                        student.classDisplayName(),
                        log.stepNo(),
                        log.content(),
                        attachmentNames,
                        log.createdAt()
                ));
            }
        }

        List<TaskDeviceRequestVO> requests = listTeacherDeviceRequests(taskId, teacher, null, null);
        Map<Long, TaskDeviceRequestEntity> requestEntities = taskDeviceRequestMapper.selectList(
                        new LambdaQueryWrapper<TaskDeviceRequestEntity>().eq(TaskDeviceRequestEntity::getTaskId, taskId))
                .stream()
                .collect(Collectors.toMap(TaskDeviceRequestEntity::getId, item -> item));
        var requestRows = requests.stream()
                .map(item -> row(
                        task.getTitle(),
                        item.studentDisplayName(),
                        item.studentUsername(),
                        item.deviceCode(),
                        item.deviceName(),
                        item.quantity(),
                        item.status(),
                        item.note(),
                        item.createdAt()
                ))
                .toList();
        var flowRows = requests.stream()
                .map(row -> {
                    TaskDeviceRequestEntity entity = requestEntities.get(row.id());
                    return row(
                            task.getTitle(),
                            row.studentDisplayName(),
                            row.studentUsername(),
                            row.deviceCode(),
                            row.deviceName(),
                            row.quantity(),
                            row.status(),
                            resolveUserDisplayName(entity == null ? null : entity.getApprovedBy()),
                            row.approvedAt(),
                            resolveUserDisplayName(entity == null ? null : entity.getRejectedBy()),
                            entity == null ? null : entity.getRejectedAt(),
                            resolveUserDisplayName(entity == null ? null : entity.getCheckoutBy()),
                            row.checkoutAt(),
                            resolveUserDisplayName(entity == null ? null : entity.getReturnBy()),
                            row.returnAt()
                    );
                })
                .toList();

        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec(
                        "筛选条件",
                        List.of("字段", "值"),
                        List.of(
                                row("任务ID", taskId),
                                row("任务标题", task.getTitle()),
                                row("导出时间", LocalDateTime.now()),
                                row("操作者", teacher.username())
                        )
                ),
                new ExcelExportService.SheetSpec(
                        "进度日志明细",
                        List.of("任务", "学生姓名", "用户名", "班级", "步骤号", "内容", "附件", "记录时间"),
                        progressRows
                ),
                new ExcelExportService.SheetSpec(
                        "完成登记明细",
                        List.of("任务", "学生姓名", "用户名", "班级", "完成状态", "完成来源", "申请时间", "确认时间", "确认教师"),
                        completionRows
                ),
                new ExcelExportService.SheetSpec(
                        "设备申请明细",
                        List.of("任务", "学生姓名", "用户名", "设备编码", "设备名称", "数量", "状态", "备注", "申请时间"),
                        requestRows
                ),
                new ExcelExportService.SheetSpec(
                        "审批流转明细",
                        List.of("任务", "学生姓名", "用户名", "设备编码", "设备名称", "数量", "当前状态", "审批人", "审批时间", "驳回人", "驳回时间", "借出登记人", "借出时间", "归还登记人", "归还时间"),
                        flowRows
                )
        ));
    }

    public DownloadData downloadProgressAttachment(Long attachmentId, AuthenticatedUser user) {
        TaskProgressAttachmentEntity entity = taskProgressAttachmentMapper.selectById(attachmentId);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "步骤附件不存在");
        }
        TaskProgressLogEntity log = taskProgressLogMapper.selectById(entity.getProgressLogId());
        if (log == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "步骤记录不存在");
        }
        ensureCanAccessProgress(log, user);
        return new DownloadData(entity.getFileName(), entity.getContentType(), fileStorageService.readBytes(entity.getRelativePath()));
    }

    private void ensureCanAccessProgress(TaskProgressLogEntity log, AuthenticatedUser user) {
        if (user.roleCodes().contains("ROLE_ADMIN")) return;
        if (user.roleCodes().contains("ROLE_TEACHER")) {
            ensureTeacherCanManageTask(log.getTaskId(), user);
            return;
        }
        if (!Objects.equals(log.getStudentId(), user.userId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权访问该步骤附件");
        }
    }

    private List<TaskProgressVO> toProgressVos(Long taskId, Long studentId) {
        return taskProgressLogMapper.findByTaskAndStudent(taskId, studentId).stream()
                .map(log -> new TaskProgressVO(
                        log.getId(),
                        log.getTaskId(),
                        log.getStudentId(),
                        log.getStepNo(),
                        log.getContent(),
                        log.getCreatedAt(),
                        taskProgressAttachmentMapper.findByProgressLogId(log.getId()).stream().map(this::toAttachmentVo).toList()
                ))
                .toList();
    }

    private ProgressAttachmentVO toAttachmentVo(TaskProgressAttachmentEntity entity) {
        return new ProgressAttachmentVO(
                entity.getId(),
                entity.getProgressLogId(),
                entity.getFileName(),
                entity.getFileSize(),
                entity.getContentType(),
                entity.getCreatedAt()
        );
    }

    private TaskCompletionVO toCompletionVo(Long taskId, Long studentId) {
        TaskCompletionEntity entity = taskCompletionMapper.findByTaskAndStudent(taskId, studentId);
        if (entity == null) {
            return new TaskCompletionVO(taskId, studentId, COMPLETION_NONE, null, null, null, null, null);
        }
        return new TaskCompletionVO(
                taskId,
                studentId,
                entity.getStatus(),
                entity.getCompletionSource(),
                entity.getRequestedAt(),
                entity.getConfirmedAt(),
                entity.getConfirmedBy(),
                resolveUserDisplayName(entity.getConfirmedBy())
        );
    }

    private String normalizeCompletionSource(String source, boolean allowTeacherDirectFallback) {
        if (COMPLETION_SOURCE_STUDENT_REQUEST.equals(source)) return COMPLETION_SOURCE_STUDENT_REQUEST;
        if (COMPLETION_SOURCE_TEACHER_DIRECT.equals(source)) return COMPLETION_SOURCE_TEACHER_DIRECT;
        return allowTeacherDirectFallback ? COMPLETION_SOURCE_TEACHER_DIRECT : COMPLETION_SOURCE_STUDENT_REQUEST;
    }

    private String completionStatusText(String status) {
        if (COMPLETION_CONFIRMED.equals(status)) return "已确认";
        if (COMPLETION_PENDING.equals(status)) return "待确认";
        return "未登记";
    }

    private String completionSourceText(String source) {
        if (COMPLETION_SOURCE_TEACHER_DIRECT.equals(source)) return "教师直接登记完成";
        if (COMPLETION_SOURCE_STUDENT_REQUEST.equals(source)) return "学生申请完成";
        return "未登记";
    }

    private List<TaskDeviceConfigVO> buildTaskDeviceConfigs(Long taskId, boolean studentOnlyConfigured) {
        Map<Long, TaskDeviceConfigEntity> configured = taskDeviceConfigMapper.findByTaskId(taskId).stream()
                .collect(Collectors.toMap(TaskDeviceConfigEntity::getDeviceId, x -> x));
        List<DeviceEntity> devices = deviceMapper.selectList(new LambdaQueryWrapper<DeviceEntity>().orderByAsc(DeviceEntity::getCode));
        return devices.stream()
                .map(device -> {
                    TaskDeviceConfigEntity config = configured.get(device.getId());
                    int configuredQuantity = config == null || config.getMaxQuantity() == null ? 0 : config.getMaxQuantity();
                    int reservedTask = nvl(taskDeviceRequestMapper.sumReservedByTaskAndDevice(taskId, device.getId()));
                    int available = availableQuantity(taskId, device, configuredQuantity, null);
                    return new TaskDeviceConfigVO(
                            device.getId(),
                            device.getCode(),
                            device.getName(),
                            device.getStatus(),
                            nvl(device.getTotalQuantity()),
                            configuredQuantity,
                            reservedTask,
                            available
                    );
                })
                .filter(item -> !studentOnlyConfigured || item.configuredQuantity() > 0)
                .toList();
    }

    private void validateDeviceRequestCapacityLocked(Long taskId, Long deviceId, Integer quantity, Long currentRequestId) {
        if (deviceId == null || quantity == null || quantity <= 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "设备和数量不能为空");
        }
        DeviceEntity device = deviceMapper.findByIdForUpdate(deviceId);
        if (device == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "设备不存在");
        }
        TaskDeviceConfigEntity config = taskDeviceConfigMapper.findByTaskId(taskId).stream()
                .filter(x -> Objects.equals(x.getDeviceId(), deviceId))
                .findFirst()
                .orElse(null);
        if (config == null || nvl(config.getMaxQuantity()) <= 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "该任务未开放此设备申请");
        }
        int available = availableQuantity(taskId, device, config.getMaxQuantity(), currentRequestId);
        if (quantity > available) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "库存不足或超过任务允许数量");
        }
    }

    private int availableQuantity(Long taskId, DeviceEntity device, Integer configuredQuantity, Long currentRequestId) {
        int total = Math.max(0, nvl(device.getTotalQuantity()));
        int globalReserved = nvl(taskDeviceRequestMapper.sumReservedByDevice(device.getId()));
        int taskReserved = nvl(taskDeviceRequestMapper.sumReservedByTaskAndDevice(taskId, device.getId()));
        if (currentRequestId != null) {
            TaskDeviceRequestEntity current = taskDeviceRequestMapper.selectById(currentRequestId);
            if (current != null && DEVICE_RESERVED_STATUSES.contains(String.valueOf(current.getStatus()))) {
                globalReserved -= nvl(current.getQuantity());
                taskReserved -= nvl(current.getQuantity());
            }
        }
        globalReserved = Math.max(0, globalReserved);
        taskReserved = Math.max(0, taskReserved);
        int globalAvailable = isDeviceBorrowable(device) ? Math.max(0, total - globalReserved) : 0;
        int taskAvailable = Math.max(0, nvl(configuredQuantity) - taskReserved);
        return Math.min(globalAvailable, taskAvailable);
    }

    private boolean isDeviceBorrowable(DeviceEntity device) {
        String status = upper(device.getStatus());
        return !"REPAIR".equals(status) && !"LOST".equals(status);
    }

    private TaskDeviceRequestEntity requireDeviceRequestEntity(Long requestId) {
        TaskDeviceRequestEntity entity = taskDeviceRequestMapper.selectById(requestId);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "设备申请不存在");
        }
        return entity;
    }

    private TaskDeviceRequestEntity requireDeviceRequestEntityForUpdate(Long requestId) {
        TaskDeviceRequestEntity entity = taskDeviceRequestMapper.findByIdForUpdate(requestId);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "设备申请不存在");
        }
        return entity;
    }

    private TaskDeviceRequestVO requireDeviceRequestVo(Long requestId, Long actorUserId, boolean teacherView) {
        TaskDeviceRequestEntity entity = requireDeviceRequestEntity(requestId);
        List<TaskDeviceRequestVO> rows = teacherView
                ? taskDeviceRequestMapper.findByTaskId(entity.getTaskId())
                : taskDeviceRequestMapper.findByTaskAndStudent(entity.getTaskId(), actorUserId);
        return rows.stream()
                .filter(x -> Objects.equals(x.id(), requestId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "设备申请读取失败"));
    }

    private void ensureStudentCanAccessTask(Long taskId, AuthenticatedUser student) {
        requireTask(taskId);
        requireStudentUser(student.userId());
        Integer accessible = expTaskMapper.countStudentAccessibleTask(taskId, student.userId());
        if (accessible == null || accessible <= 0) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权访问该任务");
        }
    }

    private void ensureStudentCanWriteTask(Long taskId, AuthenticatedUser student) {
        ensureStudentCanAccessTask(taskId, student);
        ExpTaskEntity task = requireTask(taskId);
        LocalDateTime now = LocalDateTime.now();
        if (!"OPEN".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "任务已关闭，无法继续操作");
        }
        if (task.getDeadlineAt() != null && now.isAfter(task.getDeadlineAt())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "已超过截止时间，无法继续操作");
        }
    }

    private void ensureTeacherCanManageTask(Long taskId, AuthenticatedUser actor) {
        if (actor.roleCodes().contains("ROLE_ADMIN")) {
            requireTask(taskId);
            return;
        }
        Long publisherId = expTaskMapper.findPublisherId(taskId);
        if (publisherId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务不存在");
        }
        if (!Objects.equals(publisherId, actor.userId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权管理该任务");
        }
    }

    private ExpTaskEntity requireTask(Long taskId) {
        ExpTaskEntity task = expTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    private SysUserEntity requireStudentUser(Long userId) {
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "学生不存在");
        }
        return user;
    }

    private Set<Long> loadTargetClassIds(Long taskId) {
        return expTaskTargetClassMapper.selectList(new LambdaQueryWrapper<ExpTaskTargetClassEntity>()
                        .eq(ExpTaskTargetClassEntity::getTaskId, taskId))
                .stream()
                .map(ExpTaskTargetClassEntity::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, String> loadClassDisplayNames(List<SysUserEntity> students) {
        Set<Long> classIds = students.stream().map(SysUserEntity::getClassId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (classIds.isEmpty()) return Map.of();
        Map<Long, OrgClassEntity> classes = orgClassMapper.selectBatchIds(classIds).stream().collect(Collectors.toMap(OrgClassEntity::getId, x -> x));
        Map<Long, String> out = new HashMap<>();
        for (Long classId : classIds) {
            OrgClassEntity clazz = classes.get(classId);
            if (clazz != null) {
                out.put(classId, ClassDisplayUtils.effectiveDisplayName(clazz.getGrade(), clazz.getName()));
            }
        }
        return out;
    }

    private String loadClassDisplayName(Long classId) {
        if (classId == null) return null;
        OrgClassEntity clazz = orgClassMapper.selectById(classId);
        return clazz == null ? null : ClassDisplayUtils.effectiveDisplayName(clazz.getGrade(), clazz.getName());
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) return null;
        SysUserEntity user = sysUserMapper.selectById(userId);
        return user == null ? null : user.getDisplayName();
    }

    private Comparator<TeacherTaskProgressStudentVO> progressStudentComparator() {
        return (a, b) -> {
            int c = compareUsernameNumeric(a.studentUsername(), b.studentUsername());
            if (c != 0) return c;
            return lower(a.studentDisplayName()).compareTo(lower(b.studentDisplayName()));
        };
    }

    private int compareUsernameNumeric(String a, String b) {
        boolean ad = a != null && a.matches("\\d+");
        boolean bd = b != null && b.matches("\\d+");
        if (ad && bd) {
            try {
                return Long.compare(Long.parseLong(a), Long.parseLong(b));
            } catch (NumberFormatException ignored) {
            }
        }
        return lower(a).compareTo(lower(b));
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private List<?> row(Object... values) {
        List<Object> row = new ArrayList<>();
        for (Object value : values) {
            row.add(value);
        }
        return row;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private List<MultipartFile> normalizeFiles(MultipartFile[] files) {
        List<MultipartFile> out = new ArrayList<>();
        if (files == null) return out;
        Set<String> seen = new HashSet<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            try {
                String sha = HashUtils.sha256Hex(file.getBytes());
                if (seen.add(sha)) out.add(file);
            } catch (Exception ignored) {
                out.add(file);
            }
        }
        return out;
    }

    private String cell(Object value) {
        if (value == null) return "";
        String text = Objects.toString(value, "").replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    public record DownloadData(String filename, String contentType, byte[] bytes) {
    }
}
