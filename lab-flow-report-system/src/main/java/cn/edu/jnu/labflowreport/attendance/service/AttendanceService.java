package cn.edu.jnu.labflowreport.attendance.service;

import cn.edu.jnu.labflowreport.admin.dto.PageResult;
import cn.edu.jnu.labflowreport.attendance.dto.AttendanceManualCheckinRequest;
import cn.edu.jnu.labflowreport.attendance.dto.AttendanceSessionCreateRequest;
import cn.edu.jnu.labflowreport.attendance.dto.AttendanceTokenTtlUpdateRequest;
import cn.edu.jnu.labflowreport.attendance.entity.AttendanceRecordEntity;
import cn.edu.jnu.labflowreport.attendance.entity.AttendanceSessionEntity;
import cn.edu.jnu.labflowreport.attendance.entity.AttendanceSessionRosterEntity;
import cn.edu.jnu.labflowreport.attendance.mapper.AttendanceRecordMapper;
import cn.edu.jnu.labflowreport.attendance.mapper.AttendanceSessionRosterMapper;
import cn.edu.jnu.labflowreport.attendance.mapper.AttendanceSessionMapper;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceRecordVO;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceSessionRosterRow;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceSessionVO;
import cn.edu.jnu.labflowreport.attendance.vo.TeacherAttendanceSessionDetailVO;
import cn.edu.jnu.labflowreport.attendance.vo.TeacherAttendanceSessionListItemVO;
import cn.edu.jnu.labflowreport.attendance.vo.TeacherAttendanceStudentStatusVO;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.export.ExcelExportService;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.util.ClassDisplayUtils;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseSlotEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseSlotInstanceEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseEnrollmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseSlotInstanceMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseSlotMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.schedule.entity.CourseScheduleEntity;
import cn.edu.jnu.labflowreport.schedule.mapper.CourseScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private static final String SOURCE_CLASS_SCHEDULE = "CLASS_SCHEDULE";
    private static final String SOURCE_EXPERIMENT_COURSE = "EXPERIMENT_COURSE";

    private final AttendanceSessionMapper sessionMapper;
    private final AttendanceSessionRosterMapper sessionRosterMapper;
    private final AttendanceRecordMapper recordMapper;
    private final AttendanceTokenService tokenService;
    private final CourseScheduleMapper courseScheduleMapper;
    private final SysUserMapper sysUserMapper;
    private final ExperimentCourseMapper experimentCourseMapper;
    private final ExperimentCourseSlotMapper experimentCourseSlotMapper;
    private final ExperimentCourseSlotInstanceMapper experimentCourseSlotInstanceMapper;
    private final ExperimentCourseEnrollmentMapper experimentCourseEnrollmentMapper;
    private final ExcelExportService excelExportService;
    private final SecureRandom random = new SecureRandom();
    private final int defaultTokenTtlSeconds;

    public AttendanceService(
            AttendanceSessionMapper sessionMapper,
            AttendanceSessionRosterMapper sessionRosterMapper,
            AttendanceRecordMapper recordMapper,
            AttendanceTokenService tokenService,
            CourseScheduleMapper courseScheduleMapper,
            SysUserMapper sysUserMapper,
            ExperimentCourseMapper experimentCourseMapper,
            ExperimentCourseSlotMapper experimentCourseSlotMapper,
            ExperimentCourseSlotInstanceMapper experimentCourseSlotInstanceMapper,
            ExperimentCourseEnrollmentMapper experimentCourseEnrollmentMapper,
            ExcelExportService excelExportService,
            @Value("${ATT_TOKEN_TTL_SECONDS:6}") int defaultTokenTtlSeconds
    ) {
        this.sessionMapper = sessionMapper;
        this.sessionRosterMapper = sessionRosterMapper;
        this.recordMapper = recordMapper;
        this.tokenService = tokenService;
        this.courseScheduleMapper = courseScheduleMapper;
        this.sysUserMapper = sysUserMapper;
        this.experimentCourseMapper = experimentCourseMapper;
        this.experimentCourseSlotMapper = experimentCourseSlotMapper;
        this.experimentCourseSlotInstanceMapper = experimentCourseSlotInstanceMapper;
        this.experimentCourseEnrollmentMapper = experimentCourseEnrollmentMapper;
        this.excelExportService = excelExportService;
        this.defaultTokenTtlSeconds = clampTtl(defaultTokenTtlSeconds);
    }

    @Transactional
    public AttendanceSessionVO createSession(AuthenticatedUser actor, AttendanceSessionCreateRequest request) {
        if (!actor.roleCodes().contains("ROLE_TEACHER") && !actor.roleCodes().contains("ROLE_ADMIN")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权限开启签到");
        }

        AttendanceSessionEntity existing;
        AttendanceSessionEntity entity = new AttendanceSessionEntity();
        entity.setStatus("OPEN");
        entity.setTokenTtlSeconds(resolveRequestedTtl(request.tokenTtlSeconds()));
        entity.setStartedAt(LocalDateTime.now());
        entity.setEndedAt(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        if (request.experimentCourseInstanceId() != null) {
            ExperimentCourseSlotInstanceEntity instance = experimentCourseSlotInstanceMapper.selectById(request.experimentCourseInstanceId());
            if (instance == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程课次不存在");
            }
            ExperimentCourseSlotEntity slot = experimentCourseSlotMapper.selectById(instance.getSlotGroupId());
            if (slot == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程场次不存在");
            }
            ExperimentCourseEntity course = experimentCourseMapper.selectById(instance.getCourseId());
            if (course == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程不存在");
            }
            if (actor.roleCodes().contains("ROLE_TEACHER") && !course.getTeacherId().equals(actor.userId())) {
                throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "只能开启自己实验课程课次的签到");
            }
            existing = sessionMapper.findOpenByExperimentInstanceId(instance.getId());
            entity.setSourceType(SOURCE_EXPERIMENT_COURSE);
            entity.setScheduleId(null);
            entity.setSemesterId(course.getSemesterId());
            entity.setClassId(null);
            entity.setTeacherId(course.getTeacherId());
            entity.setExperimentCourseId(course.getId());
            entity.setExperimentCourseSlotId(slot.getId());
            entity.setExperimentCourseInstanceId(instance.getId());
        } else {
            Long scheduleId = request.scheduleId();
            Long semesterId;
            Long classId;
            Long teacherId;

            if (scheduleId != null) {
                CourseScheduleEntity schedule = courseScheduleMapper.selectById(scheduleId);
                if (schedule == null) {
                    throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "课表项不存在");
                }
                if (actor.roleCodes().contains("ROLE_TEACHER") && !schedule.getTeacherId().equals(actor.userId())) {
                    throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "只能开启自己课表下的签到");
                }
                semesterId = schedule.getSemesterId();
                classId = schedule.getClassId();
                teacherId = schedule.getTeacherId();
            } else {
                if (request.semesterId() == null || request.classId() == null) {
                    throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "semesterId 和 classId 不能为空");
                }
                semesterId = request.semesterId();
                classId = request.classId();
                teacherId = actor.userId();
            }

            existing = (scheduleId != null)
                    ? sessionMapper.findOpenByScheduleId(scheduleId)
                    : sessionMapper.findOpenByKey(semesterId, classId, teacherId);
            entity.setSourceType(SOURCE_CLASS_SCHEDULE);
            entity.setScheduleId(scheduleId);
            entity.setSemesterId(semesterId);
            entity.setClassId(classId);
            entity.setTeacherId(teacherId);
            entity.setExperimentCourseId(null);
            entity.setExperimentCourseSlotId(null);
            entity.setExperimentCourseInstanceId(null);
        }

        if (existing != null) {
            ensureExperimentCourseRosterSnapshot(existing);
            return toVo(existing);
        }

        sessionMapper.insert(entity);
        ensureExperimentCourseRosterSnapshot(entity);
        AttendanceSessionVO vo = toVo(entity);
        vo.setId(entity.getId());
        return vo;
    }

    public int getSessionTokenTtlSeconds(AttendanceSessionEntity session) {
        Integer v = session.getTokenTtlSeconds();
        return clampTtl(v == null ? defaultTokenTtlSeconds : v);
    }

    @Transactional
    public AttendanceSessionVO updateSessionTokenTtl(AuthenticatedUser actor, Long sessionId, AttendanceTokenTtlUpdateRequest request) {
        AttendanceSessionEntity session = getOpenSessionOrThrow(sessionId);
        ensureTeacherOrAdmin(actor, session);
        int ttl = resolveRequestedTtl(request.tokenTtlSeconds());
        session.setTokenTtlSeconds(ttl);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return toVo(session);
    }

    @Transactional
    public AttendanceSessionVO closeSession(AuthenticatedUser actor, Long sessionId) {
        AttendanceSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "签到场次不存在");
        }
        ensureTeacherOrAdmin(actor, session);
        if ("CLOSED".equalsIgnoreCase(session.getStatus())) {
            return toVo(session);
        }
        session.setStatus("CLOSED");
        session.setEndedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return toVo(session);
    }

    public AttendanceTokenService.ParsedToken validateToken(String token) {
        return tokenService.parseAndValidate(token);
    }

    public AttendanceSessionEntity getOpenSessionOrThrow(long sessionId) {
        AttendanceSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "签到场次不存在");
        }
        if (!"OPEN".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "签到已结束");
        }
        return session;
    }

    public AttendanceSessionEntity getSessionOrThrow(long sessionId) {
        AttendanceSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "签到场次不存在");
        }
        return session;
    }

    @Transactional
    public CheckinResult checkInByToken(AuthenticatedUser student, String token, String ip, String userAgent) {
        if (!student.roleCodes().contains("ROLE_STUDENT")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "只有学生可以签到");
        }

        AttendanceTokenService.ParsedToken parsed = tokenService.parseAndValidate(token);
        AttendanceSessionEntity session = getOpenSessionOrThrow(parsed.sessionId());

        long now = Instant.now().getEpochSecond();
        int ttlSeconds = getSessionTokenTtlSeconds(session);
        if (now - parsed.issuedAtEpochSec() >= ttlSeconds) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "二维码已过期，请让老师刷新二维码重新扫码");
        }

        SysUserEntity me = requireExistingUser(student.userId());
        ensureStudentMatchesSession(student.userId(), me, session, "无法签到");

        AttendanceRecordEntity existing = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecordEntity>()
                        .eq(AttendanceRecordEntity::getSessionId, session.getId())
                        .eq(AttendanceRecordEntity::getStudentId, student.userId())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return new CheckinResult(existing.getId(), true, existing.getCheckedInAt());
        }

        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setSessionId(session.getId());
        entity.setStudentId(student.userId());
        entity.setMethod("QR");
        entity.setCheckedInAt(LocalDateTime.now());
        entity.setIp(ip);
        entity.setUserAgent(userAgent);
        entity.setOperatorId(null);
        entity.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(entity);

        return new CheckinResult(entity.getId(), false, entity.getCheckedInAt());
    }

    @Transactional
    public String getOrCreateStaticCode(AuthenticatedUser actor, Long sessionId) {
        AttendanceSessionEntity session = getOpenSessionOrThrow(sessionId);
        ensureTeacherOrAdmin(actor, session);
        String existing = session.getStaticCode();
        if (existing != null && !existing.isBlank()) {
            return existing.trim();
        }

        String code = generateStaticCodeUnique();
        session.setStaticCode(code);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return code;
    }

    @Transactional
    public CheckinResult checkInByStaticCode(AuthenticatedUser student, String code, String ip, String userAgent) {
        if (!student.roleCodes().contains("ROLE_STUDENT")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "只有学生可以签到");
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "code 不能为空");
        }

        AttendanceSessionEntity session = sessionMapper.findOpenByStaticCode(code.trim());
        if (session == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "二维码已过期，请让老师刷新二维码重新扫码");
        }

        SysUserEntity me = requireExistingUser(student.userId());
        ensureStudentMatchesSession(student.userId(), me, session, "无法签到");

        AttendanceRecordEntity existing = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecordEntity>()
                        .eq(AttendanceRecordEntity::getSessionId, session.getId())
                        .eq(AttendanceRecordEntity::getStudentId, student.userId())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return new CheckinResult(existing.getId(), true, existing.getCheckedInAt());
        }

        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setSessionId(session.getId());
        entity.setStudentId(student.userId());
        entity.setMethod("STATIC_QR");
        entity.setCheckedInAt(LocalDateTime.now());
        entity.setIp(ip);
        entity.setUserAgent(userAgent);
        entity.setOperatorId(null);
        entity.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(entity);

        return new CheckinResult(entity.getId(), false, entity.getCheckedInAt());
    }

    @Transactional
    public void manualCheckIn(AuthenticatedUser actor, Long sessionId, AttendanceManualCheckinRequest request) {
        AttendanceSessionEntity session = getOpenSessionOrThrow(sessionId);
        ensureTeacherOrAdmin(actor, session);

        SysUserEntity student = requireExistingUser(request.studentId());
        ensureStudentMatchesSession(request.studentId(), student, session, "无法补签");

        AttendanceRecordEntity existing = recordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecordEntity>()
                        .eq(AttendanceRecordEntity::getSessionId, sessionId)
                        .eq(AttendanceRecordEntity::getStudentId, request.studentId())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }

        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setSessionId(sessionId);
        entity.setStudentId(request.studentId());
        entity.setMethod("MANUAL");
        entity.setCheckedInAt(LocalDateTime.now());
        entity.setIp(null);
        entity.setUserAgent(null);
        entity.setOperatorId(actor.userId());
        entity.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(entity);
    }

    public List<AttendanceRecordVO> listRecords(AuthenticatedUser actor, Long sessionId) {
        AttendanceSessionEntity session = getSessionOrThrow(sessionId);
        ensureTeacherOrAdmin(actor, session);
        return recordMapper.findRecordsBySessionId(sessionId);
    }

    public String exportRecordsCsv(AuthenticatedUser actor, Long sessionId) {
        AttendanceSessionEntity session = getSessionOrThrow(sessionId);
        ensureTeacherOrAdmin(actor, session);
        List<SysUserEntity> roster = loadRosterUsers(session);
        List<AttendanceRecordVO> records = recordMapper.findRecordsBySessionId(sessionId);

        Map<Long, AttendanceRecordVO> recordByStudentId = new HashMap<>();
        for (AttendanceRecordVO r : records) {
            if (r.getStudentId() != null) {
                recordByStudentId.put(r.getStudentId(), r);
            }
        }

        Set<Long> rosterIds = new HashSet<>();
        for (SysUserEntity u : roster) {
            if (u.getId() != null) {
                rosterIds.add(u.getId());
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("studentUsername,studentDisplayName,status,method,checkedInAt\n");

        for (SysUserEntity u : roster) {
            AttendanceRecordVO r = recordByStudentId.get(u.getId());
            boolean checkedIn = r != null;
            csv.append(csvCell(u.getUsername())).append(",");
            csv.append(csvCell(u.getDisplayName())).append(",");
            csv.append(csvCell(checkedIn ? "CHECKED_IN" : "NOT_CHECKED_IN")).append(",");
            csv.append(csvCell(checkedIn ? r.getMethod() : "")).append(",");
            csv.append(csvCell(checkedIn ? r.getCheckedInAt() : "")).append("\n");
        }

        for (AttendanceRecordVO r : records) {
            if (r.getStudentId() == null || rosterIds.contains(r.getStudentId())) {
                continue;
            }
            csv.append(csvCell(r.getStudentUsername())).append(",");
            csv.append(csvCell(r.getStudentDisplayName())).append(",");
            csv.append(csvCell("CHECKED_IN")).append(",");
            csv.append(csvCell(r.getMethod())).append(",");
            csv.append(csvCell(r.getCheckedInAt())).append("\n");
        }
        return csv.toString();
    }

    public byte[] exportRecordsExcel(AuthenticatedUser actor, Long sessionId) {
        TeacherAttendanceSessionDetailVO detail = getTeacherSessionDetail(actor, sessionId);
        var filterRows = List.of(
                row("签到场次ID", detail.getSessionId()),
                row("来源类型", detail.getSourceType()),
                row("课程名称", detail.getCourseName()),
                row("场次名称", detail.getSlotName()),
                row("上课日期", detail.getLessonDate()),
                row("实验室", detail.getLabRoomName()),
                row("开始时间", detail.getStartedAt()),
                row("结束时间", detail.getEndedAt()),
                row("导出时间", LocalDateTime.now()),
                row("操作者", actor.username())
        );
        var summaryRows = List.of(
                row(detail.getCourseName(), detail.getSlotName(), detail.getLessonDate(), detail.getLabRoomName(), detail.getCheckedInCount(), detail.getAbsentCount(), detail.getTotalCount())
        );
        var recordRows = detail.getRoster().stream()
                .map(item -> row(
                        detail.getCourseName(),
                        detail.getSlotName(),
                        detail.getLessonDate(),
                        detail.getLabRoomName(),
                        item.getStudentDisplayName(),
                        item.getStudentUsername(),
                        item.getStatus(),
                        item.getMethod(),
                        item.getCheckedInAt()
                ))
                .toList();
        var absentRows = detail.getRoster().stream()
                .filter(item -> "NOT_CHECKED_IN".equalsIgnoreCase(item.getStatus()))
                .map(item -> row(
                        detail.getCourseName(),
                        detail.getSlotName(),
                        detail.getLessonDate(),
                        detail.getLabRoomName(),
                        item.getStudentDisplayName(),
                        item.getStudentUsername()
                ))
                .toList();
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), filterRows),
                new ExcelExportService.SheetSpec(
                        "签到汇总",
                        List.of("课程", "课次", "日期", "实验室", "已签到", "未签到", "应到"),
                        summaryRows
                ),
                new ExcelExportService.SheetSpec(
                        "签到明细",
                        List.of("课程", "课次", "日期", "实验室", "学生姓名", "用户名", "签到状态", "签到方式", "签到时间"),
                        recordRows
                ),
                new ExcelExportService.SheetSpec(
                        "未签到名单",
                        List.of("课程", "课次", "日期", "实验室", "学生姓名", "用户名"),
                        absentRows
                )
        ));
    }

    private List<?> row(Object... values) {
        List<Object> row = new ArrayList<>();
        for (Object value : values) {
            row.add(value);
        }
        return row;
    }

    public PageResult<TeacherAttendanceSessionListItemVO> listTeacherSessions(
            AuthenticatedUser actor,
            String sourceType,
            Integer grade,
            Long classId,
            String roomKeyword,
            LocalDateTime from,
            LocalDateTime to,
            String status,
            int page,
            int size
    ) {
        boolean isAdmin = actor.roleCodes().contains("ROLE_ADMIN");
        boolean isTeacher = actor.roleCodes().contains("ROLE_TEACHER");
        if (!isTeacher && !isAdmin) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权限查看历史签到");
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        int offset = (safePage - 1) * safeSize;
        Long teacherId = isAdmin ? null : actor.userId();
        long total = sessionMapper.countTeacherHistorySessions(teacherId, normalizeSourceType(sourceType), grade, classId, roomKeyword, from, to, status);
        if (total <= 0) {
            return new PageResult<>(safePage, safeSize, 0, List.of());
        }
        List<TeacherAttendanceSessionListItemVO> items = sessionMapper.findTeacherHistorySessions(
                teacherId, normalizeSourceType(sourceType), grade, classId, roomKeyword, from, to, status, safeSize, offset
        );
        items.forEach(this::normalizeHistoryItem);
        return new PageResult<>(safePage, safeSize, total, items);
    }

    public TeacherAttendanceSessionDetailVO getTeacherSessionDetail(AuthenticatedUser actor, Long sessionId) {
        if (!actor.roleCodes().contains("ROLE_TEACHER") && !actor.roleCodes().contains("ROLE_ADMIN")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权限查看历史签到");
        }
        AttendanceSessionEntity session = getSessionOrThrow(sessionId);
        ensureTeacherOrAdmin(actor, session);

        TeacherAttendanceSessionListItemVO meta = sessionMapper.findHistorySessionById(sessionId);
        if (meta == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "签到场次不存在");
        }
        normalizeHistoryItem(meta);

        TeacherAttendanceSessionDetailVO detail = new TeacherAttendanceSessionDetailVO();
        detail.setSessionId(meta.getSessionId());
        detail.setSourceType(meta.getSourceType());
        detail.setCourseName(meta.getCourseName());
        detail.setClassId(meta.getClassId());
        detail.setClassDisplayName(meta.getClassDisplayName());
        detail.setGrade(meta.getGrade());
        detail.setExperimentCourseId(meta.getExperimentCourseId());
        detail.setExperimentCourseSlotId(meta.getExperimentCourseSlotId());
        detail.setExperimentCourseInstanceId(meta.getExperimentCourseInstanceId());
        detail.setLabRoomName(meta.getLabRoomName());
        detail.setLessonDate(meta.getLessonDate());
        detail.setSlotName(meta.getSlotName());
        detail.setStartedAt(meta.getStartedAt());
        detail.setEndedAt(meta.getEndedAt());
        detail.setStatus(meta.getStatus());
        detail.setCheckedInCount(meta.getCheckedInCount());
        detail.setTotalCount(meta.getTotalCount());
        detail.setAbsentCount(meta.getAbsentCount());
        detail.setRoster(buildRosterStatuses(sessionId, session));
        return detail;
    }

    public AttendanceSessionVO toVo(AttendanceSessionEntity entity) {
        AttendanceSessionVO vo = new AttendanceSessionVO();
        vo.setId(entity.getId());
        vo.setSourceType(entity.getSourceType());
        vo.setScheduleId(entity.getScheduleId());
        vo.setSemesterId(entity.getSemesterId());
        vo.setClassId(entity.getClassId());
        vo.setTeacherId(entity.getTeacherId());
        vo.setExperimentCourseId(entity.getExperimentCourseId());
        vo.setExperimentCourseSlotId(entity.getExperimentCourseSlotId());
        vo.setExperimentCourseInstanceId(entity.getExperimentCourseInstanceId());
        vo.setStatus(entity.getStatus());
        vo.setTokenTtlSeconds(entity.getTokenTtlSeconds());
        vo.setStartedAt(entity.getStartedAt());
        vo.setEndedAt(entity.getEndedAt());
        return vo;
    }

    private void ensureTeacherOrAdmin(AuthenticatedUser actor, AttendanceSessionEntity session) {
        if (actor.roleCodes().contains("ROLE_ADMIN")) {
            return;
        }
        if (actor.roleCodes().contains("ROLE_TEACHER") && session.getTeacherId() != null && session.getTeacherId().equals(actor.userId())) {
            return;
        }
        throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权限操作该签到场次");
    }

    private SysUserEntity requireExistingUser(Long userId) {
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户不存在或已被删除");
        }
        return user;
    }

    private void ensureStudentMatchesSession(Long studentId, SysUserEntity user, AttendanceSessionEntity session, String suffix) {
        if (SOURCE_EXPERIMENT_COURSE.equalsIgnoreCase(session.getSourceType())) {
            long snapshotSize = sessionRosterMapper.countBySessionId(session.getId());
            boolean matched = snapshotSize > 0
                    ? nvl(sessionRosterMapper.countBySessionAndStudent(session.getId(), studentId)) > 0
                    : session.getExperimentCourseSlotId() != null
                        && nvl(experimentCourseEnrollmentMapper.countActiveBySlotAndStudent(session.getExperimentCourseSlotId(), studentId)) > 0;
            if (!matched) {
                throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "不属于该实验课程场次名单，" + suffix);
            }
            return;
        }
        if (user.getClassId() == null || !user.getClassId().equals(session.getClassId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "不属于该班级，" + suffix);
        }
    }

    private List<SysUserEntity> loadRosterUsers(AttendanceSessionEntity session) {
        return loadRosterRows(session).stream()
                .map(row -> {
                    SysUserEntity user = new SysUserEntity();
                    user.setId(row.studentId());
                    user.setUsername(row.studentUsername());
                    user.setDisplayName(row.studentDisplayName());
                    user.setClassId(row.classId());
                    return user;
                })
                .toList();
    }

    private List<AttendanceSessionRosterRow> loadRosterRows(AttendanceSessionEntity session) {
        if (SOURCE_EXPERIMENT_COURSE.equalsIgnoreCase(session.getSourceType())) {
            List<AttendanceSessionRosterRow> snapshotRows = sessionRosterMapper.findRowsBySessionId(session.getId());
            if (!snapshotRows.isEmpty()) {
                return snapshotRows;
            }
            if (session.getExperimentCourseSlotId() == null) {
                return List.of();
            }
            return experimentCourseEnrollmentMapper.findActiveStudentsBySlotId(session.getExperimentCourseSlotId()).stream()
                    .map(user -> new AttendanceSessionRosterRow(
                            user.getId(),
                            user.getUsername(),
                            user.getDisplayName(),
                            user.getClassId()))
                    .toList();
        }
        if (session.getClassId() == null) {
            return List.of();
        }
        return sysUserMapper.findStudentsByClassId(session.getClassId()).stream()
                .map(user -> new AttendanceSessionRosterRow(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getClassId()))
                .toList();
    }

    private String csvCell(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private List<TeacherAttendanceStudentStatusVO> buildRosterStatuses(Long sessionId, AttendanceSessionEntity session) {
        List<AttendanceSessionRosterRow> roster = loadRosterRows(session);
        List<AttendanceRecordVO> records = recordMapper.findRecordsBySessionId(sessionId);
        Map<Long, AttendanceRecordVO> recordByStudentId = new HashMap<>();
        for (AttendanceRecordVO record : records) {
            if (record.getStudentId() != null && !recordByStudentId.containsKey(record.getStudentId())) {
                recordByStudentId.put(record.getStudentId(), record);
            }
        }

        List<TeacherAttendanceStudentStatusVO> result = new ArrayList<>();
        Set<Long> rosterIds = new HashSet<>();
        for (AttendanceSessionRosterRow user : roster) {
            rosterIds.add(user.studentId());
            AttendanceRecordVO record = recordByStudentId.get(user.studentId());
            result.add(new TeacherAttendanceStudentStatusVO(
                    user.studentId(),
                    user.studentUsername(),
                    user.studentDisplayName(),
                    record == null ? "NOT_CHECKED_IN" : "CHECKED_IN",
                    record == null ? null : record.getMethod(),
                    record == null ? null : record.getCheckedInAt()
            ));
        }
        for (AttendanceRecordVO record : records) {
            if (record.getStudentId() == null || rosterIds.contains(record.getStudentId())) {
                continue;
            }
            result.add(new TeacherAttendanceStudentStatusVO(
                    record.getStudentId(),
                    record.getStudentUsername(),
                    record.getStudentDisplayName(),
                    "CHECKED_IN",
                    record.getMethod(),
                    record.getCheckedInAt()
            ));
        }
        return result;
    }

    private void fillAbsentCount(TeacherAttendanceSessionListItemVO item) {
        if (item.getGrade() == null && SOURCE_CLASS_SCHEDULE.equalsIgnoreCase(item.getSourceType())) {
            item.setGrade(ClassDisplayUtils.effectiveGrade(null, item.getClassDisplayName()));
        }
        item.setAbsentCount(Math.max(0, item.getTotalCount() - item.getCheckedInCount()));
    }

    private void normalizeHistoryItem(TeacherAttendanceSessionListItemVO item) {
        fillAbsentCount(item);
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return null;
        }
        String normalized = sourceType.trim().toUpperCase();
        if (!SOURCE_CLASS_SCHEDULE.equals(normalized) && !SOURCE_EXPERIMENT_COURSE.equals(normalized)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "sourceType 只能是 CLASS_SCHEDULE 或 EXPERIMENT_COURSE");
        }
        return normalized;
    }

    private String generateStaticCodeUnique() {
        for (int i = 0; i < 5; i++) {
            String code = randomStaticCode();
            AttendanceSessionEntity exists = sessionMapper.findOpenByStaticCode(code);
            if (exists == null) {
                return code;
            }
        }
        return randomStaticCode();
    }

    private String randomStaticCode() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private int resolveRequestedTtl(Integer requested) {
        if (requested == null) return defaultTokenTtlSeconds;
        if (requested < 3 || requested > 60) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "tokenTtlSeconds 必须在 3 到 60 秒之间");
        }
        return requested;
    }

    private int clampTtl(int ttlSeconds) {
        int v = ttlSeconds;
        if (v < 3) v = 3;
        if (v > 60) v = 60;
        return v;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private void ensureExperimentCourseRosterSnapshot(AttendanceSessionEntity session) {
        if (!SOURCE_EXPERIMENT_COURSE.equalsIgnoreCase(session.getSourceType()) || session.getId() == null) {
            return;
        }
        if (sessionRosterMapper.countBySessionId(session.getId()) > 0) {
            return;
        }
        if (session.getExperimentCourseSlotId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SysUserEntity student : experimentCourseEnrollmentMapper.findActiveStudentsBySlotId(session.getExperimentCourseSlotId())) {
            AttendanceSessionRosterEntity entity = new AttendanceSessionRosterEntity();
            entity.setSessionId(session.getId());
            entity.setStudentId(student.getId());
            entity.setClassId(student.getClassId());
            entity.setCreatedAt(now);
            sessionRosterMapper.insert(entity);
        }
    }

    public record CheckinResult(Long recordId, boolean alreadyCheckedIn, LocalDateTime checkedInAt) {
    }
}
