package cn.edu.jnu.labflowreport.schedule.service;

import cn.edu.jnu.labflowreport.admin.service.AdminAuditService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.LabRoomEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.LabRoomMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.schedule.dto.AdminCourseScheduleRequest;
import cn.edu.jnu.labflowreport.schedule.dto.AdminTimeSlotRequest;
import cn.edu.jnu.labflowreport.schedule.entity.CourseScheduleEntity;
import cn.edu.jnu.labflowreport.schedule.entity.TimeSlotEntity;
import cn.edu.jnu.labflowreport.schedule.mapper.CourseScheduleMapper;
import cn.edu.jnu.labflowreport.schedule.mapper.TimeSlotMapper;
import cn.edu.jnu.labflowreport.schedule.vo.CourseScheduleVO;
import cn.edu.jnu.labflowreport.schedule.vo.TeacherWeekScheduleItemVO;
import cn.edu.jnu.labflowreport.schedule.vo.TimeSlotVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final TimeSlotMapper timeSlotMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final SemesterMapper semesterMapper;
    private final OrgClassMapper orgClassMapper;
    private final SysUserMapper sysUserMapper;
    private final LabRoomMapper labRoomMapper;
    private final AdminAuditService adminAuditService;

    public ScheduleService(
            TimeSlotMapper timeSlotMapper,
            CourseScheduleMapper courseScheduleMapper,
            SemesterMapper semesterMapper,
            OrgClassMapper orgClassMapper,
            SysUserMapper sysUserMapper,
            LabRoomMapper labRoomMapper,
            AdminAuditService adminAuditService
    ) {
        this.timeSlotMapper = timeSlotMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.semesterMapper = semesterMapper;
        this.orgClassMapper = orgClassMapper;
        this.sysUserMapper = sysUserMapper;
        this.labRoomMapper = labRoomMapper;
        this.adminAuditService = adminAuditService;
    }

    public List<TimeSlotVO> listTimeSlots() {
        return timeSlotMapper.findAllOrdered().stream().map(this::toVo).toList();
    }

    @Transactional
    public TimeSlotVO createTimeSlot(AuthenticatedUser actor, AdminTimeSlotRequest request) {
        LocalTime start = parseTime(request.startTime());
        LocalTime end = parseTime(request.endTime());
        if (!end.isAfter(start)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "endTime 必须晚于 startTime");
        }

        TimeSlotEntity entity = new TimeSlotEntity();
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        timeSlotMapper.insert(entity);

        adminAuditService.record(actor, "TIME_SLOT_CREATE", "time_slot", entity.getId(),
                Map.of("code", entity.getCode(), "name", entity.getName()));

        TimeSlotVO vo = toVo(entity);
        vo.setId(entity.getId());
        return vo;
    }

    @Transactional
    public TimeSlotVO updateTimeSlot(AuthenticatedUser actor, Long id, AdminTimeSlotRequest request) {
        TimeSlotEntity entity = timeSlotMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "节次不存在");
        }
        LocalTime start = parseTime(request.startTime());
        LocalTime end = parseTime(request.endTime());
        if (!end.isAfter(start)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "endTime 必须晚于 startTime");
        }

        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setUpdatedAt(LocalDateTime.now());
        timeSlotMapper.updateById(entity);

        adminAuditService.record(actor, "TIME_SLOT_UPDATE", "time_slot", id,
                Map.of("code", entity.getCode(), "name", entity.getName()));

        return toVo(entity);
    }

    @Transactional
    public void deleteTimeSlot(AuthenticatedUser actor, Long id) {
        int deleted = timeSlotMapper.deleteById(id);
        if (deleted <= 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "节次不存在");
        }
        adminAuditService.record(actor, "TIME_SLOT_DELETE", "time_slot", id, Map.of());
    }

    public List<CourseScheduleVO> listCourseSchedules(Long semesterId, LocalDate from, LocalDate to, Long teacherId, Long classId) {
        return courseScheduleMapper.findSchedules(semesterId, from, to, teacherId, classId);
    }

    @Transactional
    public CourseScheduleVO createCourseSchedule(AuthenticatedUser actor, AdminCourseScheduleRequest request) {
        SemesterEntity semester = semesterMapper.selectById(request.semesterId());
        if (semester == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "semesterId 不存在");
        }
        OrgClassEntity clazz = orgClassMapper.selectById(request.classId());
        if (clazz == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "classId 不存在");
        }
        SysUserEntity teacher = sysUserMapper.selectById(request.teacherId());
        if (teacher == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "teacherId 不存在");
        }
        if (request.labRoomId() != null) {
            LabRoomEntity room = labRoomMapper.selectById(request.labRoomId());
            if (room == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "labRoomId 不存在");
            }
        }
        TimeSlotEntity slot = timeSlotMapper.selectById(request.slotId());
        if (slot == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "slotId 不存在");
        }
        LocalDate date = parseDate(request.lessonDate());

        CourseScheduleEntity entity = new CourseScheduleEntity();
        entity.setSemesterId(request.semesterId());
        entity.setClassId(request.classId());
        entity.setTeacherId(request.teacherId());
        entity.setLabRoomId(request.labRoomId());
        entity.setLessonDate(date);
        entity.setSlotId(request.slotId());
        entity.setCourseName(request.courseName());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        courseScheduleMapper.insert(entity);

        adminAuditService.record(actor, "COURSE_SCHEDULE_CREATE", "course_schedule", entity.getId(),
                Map.of("semesterId", request.semesterId(), "classId", request.classId(), "teacherId", request.teacherId(),
                        "lessonDate", request.lessonDate(), "slotId", request.slotId()));

        return courseScheduleMapper.findSchedules(request.semesterId(), date, date, request.teacherId(), request.classId()).stream()
                .filter(item -> item.getId().equals(entity.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "课表创建成功但读取失败"));
    }

    @Transactional
    public CourseScheduleVO updateCourseSchedule(AuthenticatedUser actor, Long id, AdminCourseScheduleRequest request) {
        CourseScheduleEntity entity = courseScheduleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "课表项不存在");
        }
        // Reuse create validations.
        createCourseSchedulePrecheck(request);

        entity.setSemesterId(request.semesterId());
        entity.setClassId(request.classId());
        entity.setTeacherId(request.teacherId());
        entity.setLabRoomId(request.labRoomId());
        entity.setLessonDate(parseDate(request.lessonDate()));
        entity.setSlotId(request.slotId());
        entity.setCourseName(request.courseName());
        entity.setUpdatedAt(LocalDateTime.now());
        courseScheduleMapper.updateById(entity);

        adminAuditService.record(actor, "COURSE_SCHEDULE_UPDATE", "course_schedule", id,
                Map.of("semesterId", request.semesterId(), "classId", request.classId(), "teacherId", request.teacherId(),
                        "lessonDate", request.lessonDate(), "slotId", request.slotId()));

        LocalDate date = entity.getLessonDate();
        return courseScheduleMapper.findSchedules(entity.getSemesterId(), date, date, entity.getTeacherId(), entity.getClassId()).stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "课表更新成功但读取失败"));
    }

    @Transactional
    public void deleteCourseSchedule(AuthenticatedUser actor, Long id) {
        int deleted = courseScheduleMapper.deleteById(id);
        if (deleted <= 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "课表项不存在");
        }
        adminAuditService.record(actor, "COURSE_SCHEDULE_DELETE", "course_schedule", id, Map.of());
    }

    public List<TeacherWeekScheduleItemVO> listTeacherWeek(Long teacherId, Long semesterId, LocalDate weekStartDate) {
        LocalDate from = weekStartDate;
        LocalDate to = weekStartDate.plusDays(6);
        return courseScheduleMapper.findTeacherWeek(teacherId, semesterId, from, to);
    }

    private void createCourseSchedulePrecheck(AdminCourseScheduleRequest request) {
        if (semesterMapper.selectById(request.semesterId()) == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "semesterId 不存在");
        }
        if (orgClassMapper.selectById(request.classId()) == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "classId 不存在");
        }
        if (sysUserMapper.selectById(request.teacherId()) == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "teacherId 不存在");
        }
        if (request.labRoomId() != null && labRoomMapper.selectById(request.labRoomId()) == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "labRoomId 不存在");
        }
        if (timeSlotMapper.selectById(request.slotId()) == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "slotId 不存在");
        }
        parseDate(request.lessonDate());
    }

    private TimeSlotVO toVo(TimeSlotEntity entity) {
        TimeSlotVO vo = new TimeSlotVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        return vo;
    }

    private LocalTime parseTime(String value) {
        String v = value == null ? "" : value.trim();
        try {
            // Accept HH:mm and HH:mm:ss.
            if (v.length() == 5) {
                return LocalTime.parse(v + ":00");
            }
            return LocalTime.parse(v);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "时间格式错误，应为 HH:mm 或 HH:mm:ss");
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "日期格式错误，应为 YYYY-MM-DD");
        }
    }
}

