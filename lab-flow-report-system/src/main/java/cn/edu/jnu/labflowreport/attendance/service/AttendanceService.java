package cn.edu.jnu.labflowreport.attendance.service;

import cn.edu.jnu.labflowreport.attendance.dto.AttendanceManualCheckinRequest;
import cn.edu.jnu.labflowreport.attendance.dto.AttendanceSessionCreateRequest;
import cn.edu.jnu.labflowreport.attendance.entity.AttendanceRecordEntity;
import cn.edu.jnu.labflowreport.attendance.entity.AttendanceSessionEntity;
import cn.edu.jnu.labflowreport.attendance.mapper.AttendanceRecordMapper;
import cn.edu.jnu.labflowreport.attendance.mapper.AttendanceSessionMapper;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceRecordVO;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceSessionVO;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.schedule.entity.CourseScheduleEntity;
import cn.edu.jnu.labflowreport.schedule.mapper.CourseScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private final AttendanceSessionMapper sessionMapper;
    private final AttendanceRecordMapper recordMapper;
    private final AttendanceTokenService tokenService;
    private final CourseScheduleMapper courseScheduleMapper;
    private final SysUserMapper sysUserMapper;

    public AttendanceService(
            AttendanceSessionMapper sessionMapper,
            AttendanceRecordMapper recordMapper,
            AttendanceTokenService tokenService,
            CourseScheduleMapper courseScheduleMapper,
            SysUserMapper sysUserMapper
    ) {
        this.sessionMapper = sessionMapper;
        this.recordMapper = recordMapper;
        this.tokenService = tokenService;
        this.courseScheduleMapper = courseScheduleMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Transactional
    public AttendanceSessionVO createSession(AuthenticatedUser actor, AttendanceSessionCreateRequest request) {
        if (!actor.roleCodes().contains("ROLE_TEACHER") && !actor.roleCodes().contains("ROLE_ADMIN")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权限开启签到");
        }

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

        AttendanceSessionEntity existing = (scheduleId != null)
                ? sessionMapper.findOpenByScheduleId(scheduleId)
                : sessionMapper.findOpenByKey(semesterId, classId, teacherId);
        if (existing != null) {
            return toVo(existing);
        }

        AttendanceSessionEntity entity = new AttendanceSessionEntity();
        entity.setScheduleId(scheduleId);
        entity.setSemesterId(semesterId);
        entity.setClassId(classId);
        entity.setTeacherId(teacherId);
        entity.setStatus("OPEN");
        entity.setStartedAt(LocalDateTime.now());
        entity.setEndedAt(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(entity);

        AttendanceSessionVO vo = toVo(entity);
        vo.setId(entity.getId());
        return vo;
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

        SysUserEntity me = sysUserMapper.selectById(student.userId());
        if (me == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户不存在或已被删除");
        }
        if (me.getClassId() == null || !me.getClassId().equals(session.getClassId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "不属于该班级，无法签到");
        }

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
    public void manualCheckIn(AuthenticatedUser actor, Long sessionId, AttendanceManualCheckinRequest request) {
        AttendanceSessionEntity session = getOpenSessionOrThrow(sessionId);
        ensureTeacherOrAdmin(actor, session);

        SysUserEntity student = sysUserMapper.selectById(request.studentId());
        if (student == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "studentId 不存在");
        }
        if (student.getClassId() == null || !student.getClassId().equals(session.getClassId())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "该学生不属于本班，无法补签");
        }

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
        List<SysUserEntity> roster = sysUserMapper.findStudentsByClassId(session.getClassId());
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

        // Export full roster: who checked in and who did not.
        for (SysUserEntity u : roster) {
            AttendanceRecordVO r = recordByStudentId.get(u.getId());
            boolean checkedIn = r != null;
            csv.append(csvCell(u.getUsername())).append(",");
            csv.append(csvCell(u.getDisplayName())).append(",");
            csv.append(csvCell(checkedIn ? "CHECKED_IN" : "NOT_CHECKED_IN")).append(",");
            csv.append(csvCell(checkedIn ? r.getMethod() : "")).append(",");
            csv.append(csvCell(checkedIn ? r.getCheckedInAt() : "")).append("\n");
        }

        // Safety: if a record exists for a user not in current roster (e.g., disabled later), still include it.
        for (AttendanceRecordVO r : records) {
            if (r.getStudentId() == null) continue;
            if (rosterIds.contains(r.getStudentId())) continue;

            csv.append(csvCell(r.getStudentUsername())).append(",");
            csv.append(csvCell(r.getStudentDisplayName())).append(",");
            csv.append(csvCell("CHECKED_IN")).append(",");
            csv.append(csvCell(r.getMethod())).append(",");
            csv.append(csvCell(r.getCheckedInAt())).append("\n");
        }
        return csv.toString();
    }

    public AttendanceSessionVO toVo(AttendanceSessionEntity entity) {
        AttendanceSessionVO vo = new AttendanceSessionVO();
        vo.setId(entity.getId());
        vo.setScheduleId(entity.getScheduleId());
        vo.setSemesterId(entity.getSemesterId());
        vo.setClassId(entity.getClassId());
        vo.setTeacherId(entity.getTeacherId());
        vo.setStatus(entity.getStatus());
        vo.setStartedAt(entity.getStartedAt());
        vo.setEndedAt(entity.getEndedAt());
        return vo;
    }

    private void ensureTeacherOrAdmin(AuthenticatedUser actor, AttendanceSessionEntity session) {
        if (actor.roleCodes().contains("ROLE_ADMIN")) {
            return;
        }
        if (actor.roleCodes().contains("ROLE_TEACHER") && session.getTeacherId().equals(actor.userId())) {
            return;
        }
        throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权限操作该签到场次");
    }

    private String csvCell(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    public record CheckinResult(Long recordId, boolean alreadyCheckedIn, LocalDateTime checkedInAt) {
    }
}
