
package cn.edu.jnu.labflowreport.elective.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.edu.jnu.labflowreport.attendance.entity.AttendanceSessionEntity;
import cn.edu.jnu.labflowreport.attendance.mapper.AttendanceSessionMapper;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.util.ClassDisplayUtils;
import cn.edu.jnu.labflowreport.elective.dto.ExperimentCourseEnrollRequest;
import cn.edu.jnu.labflowreport.elective.dto.ExperimentCourseSaveRequest;
import cn.edu.jnu.labflowreport.elective.dto.TeacherExperimentCourseManualEnrollRequest;
import cn.edu.jnu.labflowreport.elective.dto.TeacherExperimentCourseRemoveStudentRequest;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseBlockedStudentVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseEnrollmentRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseRosterVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseSlotInstanceRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseSlotRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseStudentOptionVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseSummaryRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseVO;
import cn.edu.jnu.labflowreport.elective.vo.StudentEnrollmentRowVO;
import cn.edu.jnu.labflowreport.elective.vo.StudentExperimentCourseVO;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseEnrollmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseSlotEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseSlotInstanceEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseBlockedStudentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseTargetClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseTargetStudentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.LabRoomEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseEnrollmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseBlockedStudentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseSlotInstanceMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseSlotMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseTargetClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseTargetStudentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.LabRoomMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.schedule.mapper.TimeSlotMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperimentCourseService {

    private static final Set<String> SLOT_MODES = Set.of("SINGLE", "RECURRING");
    private static final Set<String> REPEAT_PATTERNS = Set.of("EVERY_WEEK", "ODD_WEEK");
    private static final Set<String> RANGE_MODES = Set.of("SEMESTER", "DATE_RANGE");

    private final ExperimentCourseMapper experimentCourseMapper;
    private final ExperimentCourseSlotMapper experimentCourseSlotMapper;
    private final ExperimentCourseSlotInstanceMapper experimentCourseSlotInstanceMapper;
    private final ExperimentCourseTargetClassMapper experimentCourseTargetClassMapper;
    private final ExperimentCourseTargetStudentMapper experimentCourseTargetStudentMapper;
    private final ExperimentCourseEnrollmentMapper experimentCourseEnrollmentMapper;
    private final ExperimentCourseBlockedStudentMapper experimentCourseBlockedStudentMapper;
    private final AttendanceSessionMapper attendanceSessionMapper;
    private final SemesterMapper semesterMapper;
    private final OrgClassMapper orgClassMapper;
    private final SysUserMapper sysUserMapper;
    private final LabRoomMapper labRoomMapper;
    private final TimeSlotMapper timeSlotMapper;

    public ExperimentCourseService(
            ExperimentCourseMapper experimentCourseMapper,
            ExperimentCourseSlotMapper experimentCourseSlotMapper,
            ExperimentCourseSlotInstanceMapper experimentCourseSlotInstanceMapper,
            ExperimentCourseTargetClassMapper experimentCourseTargetClassMapper,
            ExperimentCourseTargetStudentMapper experimentCourseTargetStudentMapper,
            ExperimentCourseEnrollmentMapper experimentCourseEnrollmentMapper,
            ExperimentCourseBlockedStudentMapper experimentCourseBlockedStudentMapper,
            AttendanceSessionMapper attendanceSessionMapper,
            SemesterMapper semesterMapper,
            OrgClassMapper orgClassMapper,
            SysUserMapper sysUserMapper,
            LabRoomMapper labRoomMapper,
            TimeSlotMapper timeSlotMapper
    ) {
        this.experimentCourseMapper = experimentCourseMapper;
        this.experimentCourseSlotMapper = experimentCourseSlotMapper;
        this.experimentCourseSlotInstanceMapper = experimentCourseSlotInstanceMapper;
        this.experimentCourseTargetClassMapper = experimentCourseTargetClassMapper;
        this.experimentCourseTargetStudentMapper = experimentCourseTargetStudentMapper;
        this.experimentCourseEnrollmentMapper = experimentCourseEnrollmentMapper;
        this.experimentCourseBlockedStudentMapper = experimentCourseBlockedStudentMapper;
        this.attendanceSessionMapper = attendanceSessionMapper;
        this.semesterMapper = semesterMapper;
        this.orgClassMapper = orgClassMapper;
        this.sysUserMapper = sysUserMapper;
        this.labRoomMapper = labRoomMapper;
        this.timeSlotMapper = timeSlotMapper;
    }

    public List<ExperimentCourseVO> listTeacherCourses(AuthenticatedUser actor) {
        return hydrateCourses(experimentCourseMapper.findTeacherCourseSummaries(actor.userId()));
    }

    @Transactional
    public void syncRecurringSlotInstances() {
        Map<Long, SemesterEntity> semesterCache = new HashMap<>();
        Map<Long, ExperimentCourseEntity> courseCache = new HashMap<>();
        Map<Long, List<ExperimentCourseSlotEntity>> slotsByCourse = experimentCourseSlotMapper.selectList(
                        new LambdaQueryWrapper<ExperimentCourseSlotEntity>().eq(ExperimentCourseSlotEntity::getMode, "RECURRING"))
                .stream()
                .collect(Collectors.groupingBy(ExperimentCourseSlotEntity::getCourseId));
        for (Map.Entry<Long, List<ExperimentCourseSlotEntity>> entry : slotsByCourse.entrySet()) {
            ExperimentCourseEntity course = courseCache.computeIfAbsent(entry.getKey(), experimentCourseMapper::selectById);
            if (course == null) {
                continue;
            }
            SemesterEntity semester = semesterCache.computeIfAbsent(course.getSemesterId(), semesterMapper::selectById);
            if (semester == null) {
                continue;
            }
            List<ExperimentCourseSlotEntity> slots = entry.getValue().stream()
                    .sorted(Comparator.comparing(ExperimentCourseSlotEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(ExperimentCourseSlotEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            for (int i = 0; i < slots.size(); i++) {
                if (slotHasAttendanceHistory(slots.get(i).getId())) {
                    continue;
                }
                rebuildInstances(slots.get(i), semester, i + 1);
            }
        }
    }

    @Transactional
    public ExperimentCourseVO createTeacherCourse(AuthenticatedUser actor, ExperimentCourseSaveRequest request) {
        SemesterEntity semester = requireSemester(request.semesterId());
        validateSaveRequest(request, semester);
        ExperimentCourseEntity entity = new ExperimentCourseEntity();
        entity.setTitle(request.title().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setTeacherId(actor.userId());
        entity.setSemesterId(request.semesterId());
        entity.setStatus("OPEN");
        entity.setEnrollDeadlineAt(request.enrollDeadlineAt());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        experimentCourseMapper.insert(entity);
        replaceTargets(entity.getId(), request);
        replaceSlots(entity.getId(), request, semester);
        return requireCourseVo(entity.getId());
    }

    @Transactional
    public ExperimentCourseVO updateTeacherCourse(Long courseId, AuthenticatedUser actor, ExperimentCourseSaveRequest request) {
        SemesterEntity semester = requireSemester(request.semesterId());
        validateSaveRequest(request, semester);
        ExperimentCourseEntity entity = requireManageableCourse(courseId, actor);
        entity.setTitle(request.title().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setSemesterId(request.semesterId());
        entity.setEnrollDeadlineAt(request.enrollDeadlineAt());
        entity.setUpdatedAt(LocalDateTime.now());
        experimentCourseMapper.updateById(entity);
        replaceTargets(courseId, request);
        replaceSlots(courseId, request, semester);
        return requireCourseVo(courseId);
    }

    @Transactional
    public ExperimentCourseVO updateTeacherCourseStatus(Long courseId, AuthenticatedUser actor, String status) {
        ExperimentCourseEntity entity = requireManageableCourse(courseId, actor);
        String normalized = upper(status);
        if (!"OPEN".equals(normalized) && !"CLOSED".equals(normalized)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "status 只能是 OPEN 或 CLOSED");
        }
        entity.setStatus(normalized);
        entity.setUpdatedAt(LocalDateTime.now());
        experimentCourseMapper.updateById(entity);
        return requireCourseVo(courseId);
    }

    public List<ExperimentCourseEnrollmentRowVO> listTeacherEnrollments(Long courseId, AuthenticatedUser actor) {
        requireManageableCourse(courseId, actor);
        return experimentCourseSlotMapper.findEnrollmentRowsByCourseId(courseId);
    }

    public ExperimentCourseRosterVO getTeacherCourseRoster(Long courseId, AuthenticatedUser actor) {
        requireManageableCourse(courseId, actor);
        return new ExperimentCourseRosterVO(
                experimentCourseSlotMapper.findEnrollmentRowsByCourseId(courseId),
                experimentCourseBlockedStudentMapper.findRowsByCourseId(courseId)
        );
    }

    public List<ExperimentCourseStudentOptionVO> listTeacherSlotRoster(Long slotId, AuthenticatedUser actor) {
        ExperimentCourseSlotEntity slot = experimentCourseSlotMapper.selectById(slotId);
        if (slot == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程场次不存在");
        }
        requireManageableCourse(slot.getCourseId(), actor);
        Map<Long, OrgClassEntity> classMap = loadClassMap(experimentCourseEnrollmentMapper.findActiveStudentsBySlotId(slotId));
        return experimentCourseEnrollmentMapper.findActiveStudentsBySlotId(slotId).stream()
                .map(user -> new ExperimentCourseStudentOptionVO(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        displayClass(classMap.get(user.getClassId()))
                ))
                .toList();
    }

    public List<StudentExperimentCourseVO> listStudentAvailableCourses(AuthenticatedUser student) {
        SysUserEntity user = requireStudent(student.userId());
        Map<Long, StudentEnrollmentRowVO> enrolled = experimentCourseEnrollmentMapper.findActiveRowsByStudentId(student.userId()).stream()
                .collect(Collectors.toMap(StudentEnrollmentRowVO::courseId, x -> x, (a, b) -> a));
        Set<Long> blockedCourseIds = new LinkedHashSet<>(experimentCourseBlockedStudentMapper.findCourseIdsByStudentId(student.userId()));
        return hydrateCourses(experimentCourseMapper.findOpenCourseSummaries(null)).stream()
                .filter(course -> blockedCourseIds.contains(course.getId()) || isEligible(course.getId(), user))
                .map(course -> toStudentVo(course, enrolled.get(course.getId()), blockedCourseIds.contains(course.getId())))
                .toList();
    }

    public List<StudentExperimentCourseVO> listStudentMyCourses(AuthenticatedUser student) {
        Map<Long, StudentEnrollmentRowVO> enrolled = experimentCourseEnrollmentMapper.findActiveRowsByStudentId(student.userId()).stream()
                .collect(Collectors.toMap(StudentEnrollmentRowVO::courseId, x -> x, (a, b) -> a));
        if (enrolled.isEmpty()) {
            return List.of();
        }
        return hydrateCourses(enrolled.keySet().stream()
                .map(experimentCourseMapper::findSummaryById)
                .filter(Objects::nonNull)
                .toList()).stream().map(course -> toStudentVo(course, enrolled.get(course.getId()), false)).toList();
    }

    @Transactional
    public StudentExperimentCourseVO enroll(AuthenticatedUser student, Long courseId, ExperimentCourseEnrollRequest request) {
        SysUserEntity user = requireStudent(student.userId());
        ExperimentCourseEntity course = experimentCourseMapper.findByIdForUpdate(courseId);
        if (course == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程不存在");
        }
        ensureCourseOpen(course);
        if (isBlocked(courseId, student.userId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "你已被教师移出该实验课程，暂不可自行选课");
        }
        if (!isEligible(courseId, user)) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "你不在该实验课程的开放范围内");
        }
        ExperimentCourseEnrollmentEntity existing = experimentCourseEnrollmentMapper.findByCourseAndStudentForUpdate(courseId, student.userId());
        if (existing != null && "ENROLLED".equalsIgnoreCase(existing.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "你已选过该实验课程");
        }
        ExperimentCourseSlotEntity slot = experimentCourseSlotMapper.findByIdForUpdate(request.slotId());
        if (slot == null || !Objects.equals(slot.getCourseId(), courseId)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "场次不存在或不属于该课程");
        }
        int enrolled = nvl(experimentCourseEnrollmentMapper.countEnrolledBySlotId(slot.getId()));
        if (slot.getCapacity() != null && enrolled >= slot.getCapacity()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前场次名额已满");
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            ExperimentCourseEnrollmentEntity entity = new ExperimentCourseEnrollmentEntity();
            entity.setCourseId(courseId);
            entity.setSlotId(slot.getId());
            entity.setStudentId(student.userId());
            entity.setStatus("ENROLLED");
            entity.setJoinSource("STUDENT_SELF");
            entity.setSelectedAt(now);
            experimentCourseEnrollmentMapper.insert(entity);
        } else {
            existing.setSlotId(slot.getId());
            existing.setStatus("ENROLLED");
            existing.setJoinSource("STUDENT_SELF");
            existing.setSelectedAt(now);
            existing.setRemovedAt(null);
            existing.setRemovedByTeacherId(null);
            experimentCourseEnrollmentMapper.updateById(existing);
        }
        return listStudentMyCourses(student).stream()
                .filter(item -> Objects.equals(item.getId(), courseId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "选课成功但读取失败"));
    }

    @Transactional
    public ExperimentCourseRosterVO teacherEnrollStudent(Long courseId, AuthenticatedUser actor, TeacherExperimentCourseManualEnrollRequest request) {
        ExperimentCourseEntity course = experimentCourseMapper.findByIdForUpdate(courseId);
        if (course == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程不存在");
        }
        requireManageableCourse(courseId, actor);
        SysUserEntity student = requireStudent(request.studentId());
        ExperimentCourseSlotEntity slot = experimentCourseSlotMapper.findByIdForUpdate(request.slotId());
        if (slot == null || !Objects.equals(slot.getCourseId(), courseId)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "场次不存在或不属于该课程");
        }
        removeBlocked(courseId, student.getId());
        ExperimentCourseEnrollmentEntity existing = experimentCourseEnrollmentMapper.findByCourseAndStudentForUpdate(courseId, student.getId());
        if (existing != null && "ENROLLED".equalsIgnoreCase(existing.getStatus()) && Objects.equals(existing.getSlotId(), slot.getId())) {
            existing.setJoinSource("TEACHER_MANUAL");
            experimentCourseEnrollmentMapper.updateById(existing);
            return getTeacherCourseRoster(courseId, actor);
        }
        int enrolled = nvl(experimentCourseEnrollmentMapper.countEnrolledBySlotId(slot.getId()));
        if (slot.getCapacity() != null && enrolled >= slot.getCapacity()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前场次名额已满");
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            ExperimentCourseEnrollmentEntity entity = new ExperimentCourseEnrollmentEntity();
            entity.setCourseId(courseId);
            entity.setSlotId(slot.getId());
            entity.setStudentId(student.getId());
            entity.setStatus("ENROLLED");
            entity.setJoinSource("TEACHER_MANUAL");
            entity.setSelectedAt(now);
            experimentCourseEnrollmentMapper.insert(entity);
        } else {
            existing.setSlotId(slot.getId());
            existing.setStatus("ENROLLED");
            existing.setJoinSource("TEACHER_MANUAL");
            existing.setSelectedAt(now);
            existing.setRemovedAt(null);
            existing.setRemovedByTeacherId(null);
            experimentCourseEnrollmentMapper.updateById(existing);
        }
        return getTeacherCourseRoster(courseId, actor);
    }

    @Transactional
    public ExperimentCourseRosterVO teacherRemoveStudent(Long courseId, AuthenticatedUser actor, TeacherExperimentCourseRemoveStudentRequest request) {
        requireManageableCourse(courseId, actor);
        ExperimentCourseEnrollmentEntity existing = experimentCourseEnrollmentMapper.findByCourseAndStudentForUpdate(courseId, request.studentId());
        if (existing == null || !"ENROLLED".equalsIgnoreCase(existing.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "该学生当前不在实验课程报名名单中");
        }
        existing.setStatus("REMOVED");
        existing.setRemovedAt(LocalDateTime.now());
        existing.setRemovedByTeacherId(actor.userId());
        experimentCourseEnrollmentMapper.updateById(existing);
        upsertBlocked(courseId, request.studentId(), actor.userId());
        return getTeacherCourseRoster(courseId, actor);
    }

    @Transactional
    public ExperimentCourseRosterVO unblockStudent(Long courseId, Long studentId, AuthenticatedUser actor) {
        requireManageableCourse(courseId, actor);
        removeBlocked(courseId, studentId);
        return getTeacherCourseRoster(courseId, actor);
    }
    public List<ExperimentCourseStudentOptionVO> listStudentOptions(String q) {
        String keyword = lower(q).trim();
        List<SysUserEntity> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getEnabled, true)
                .orderByAsc(SysUserEntity::getUsername));
        Map<Long, OrgClassEntity> classMap = loadClassMap(users);
        return users.stream()
                .filter(user -> sysUserMapper.findRoleCodesByUserId(user.getId()).contains("ROLE_STUDENT"))
                .filter(user -> keyword.isBlank() || lower(user.getUsername()).contains(keyword) || lower(user.getDisplayName()).contains(keyword))
                .limit(100)
                .map(user -> new ExperimentCourseStudentOptionVO(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        displayClass(classMap.get(user.getClassId()))
                ))
                .toList();
    }

    public MetaVO getMeta() {
        List<SemesterOptionVO> semesters = semesterMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SemesterEntity::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> new SemesterOptionVO(item.getId(), item.getName(), item.getStartDate(), item.getEndDate()))
                .toList();
        List<MetaOptionVO> timeSlots = timeSlotMapper.findAllOrdered().stream()
                .map(item -> new MetaOptionVO(item.getId(), item.getName()))
                .toList();
        List<MetaOptionVO> labRooms = labRoomMapper.selectList(null).stream()
                .sorted(Comparator.comparing(LabRoomEntity::getName, Comparator.nullsLast(String::compareTo)))
                .map(item -> new MetaOptionVO(item.getId(), item.getName()))
                .toList();
        return new MetaVO(semesters, timeSlots, labRooms);
    }

    public boolean isStudentEnrolledInCourse(Long courseId, Long studentId) {
        return nvl(experimentCourseEnrollmentMapper.countActiveByCourseAndStudent(courseId, studentId)) > 0;
    }

    private SemesterEntity requireSemester(Long semesterId) {
        SemesterEntity semester = semesterMapper.selectById(semesterId);
        if (semester == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "semesterId 不存在");
        }
        if (semester.getStartDate() == null || semester.getEndDate() == null || semester.getEndDate().isBefore(semester.getStartDate())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "学期日期配置不完整，无法生成重复课次");
        }
        return semester;
    }

    private void validateSaveRequest(ExperimentCourseSaveRequest request, SemesterEntity semester) {
        Set<Long> classIds = uniqIds(request.targetClassIds());
        if (!classIds.isEmpty() && orgClassMapper.selectBatchIds(classIds).size() != classIds.size()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "开放班级中存在无效班级");
        }
        Set<Long> studentIds = uniqIds(request.targetStudentIds());
        if (!studentIds.isEmpty() && sysUserMapper.selectBatchIds(studentIds).size() != studentIds.size()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "指定学生中存在无效用户");
        }
        Set<Long> requestIds = new LinkedHashSet<>();
        for (ExperimentCourseSaveRequest.ExperimentCourseSlotRequest slot : request.slots()) {
            if (slot.id() != null && !requestIds.add(slot.id())) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "存在重复的场次 ID");
            }
            validateSlotRequest(slot, semester);
        }
    }

    private void validateSlotRequest(ExperimentCourseSaveRequest.ExperimentCourseSlotRequest slot, SemesterEntity semester) {
        if (timeSlotMapper.selectById(slot.slotId()) == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "存在无效节次");
        }
        if (labRoomMapper.selectById(slot.labRoomId()) == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "存在无效实验室");
        }
        String mode = upper(slot.mode());
        if (!SLOT_MODES.contains(mode)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "场次类型只能是 SINGLE 或 RECURRING");
        }
        LocalDate firstLessonDate = slot.firstLessonDate();
        if (firstLessonDate.isBefore(semester.getStartDate()) || firstLessonDate.isAfter(semester.getEndDate())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "首次上课日期必须位于所选学期内");
        }
        if ("SINGLE".equals(mode)) {
            return;
        }
        String repeatPattern = upper(slot.repeatPattern());
        if (!REPEAT_PATTERNS.contains(repeatPattern)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "重复方式只能是 EVERY_WEEK 或 ODD_WEEK");
        }
        String rangeMode = upper(slot.rangeMode());
        if (!RANGE_MODES.contains(rangeMode)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "生效范围只能是 SEMESTER 或 DATE_RANGE");
        }
        LocalDate rangeStart = resolveRangeStart(slot, semester);
        LocalDate rangeEnd = resolveRangeEnd(slot, semester);
        if (rangeStart.isAfter(rangeEnd)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "日期区间开始时间不能晚于结束时间");
        }
        if (rangeStart.isBefore(semester.getStartDate()) || rangeEnd.isAfter(semester.getEndDate())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "多次课的日期范围必须位于所选学期内");
        }
        if (firstLessonDate.isBefore(rangeStart) || firstLessonDate.isAfter(rangeEnd)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "首次上课日期必须位于多次课生效范围内");
        }
    }

    private ExperimentCourseEntity requireManageableCourse(Long courseId, AuthenticatedUser actor) {
        ExperimentCourseEntity entity = experimentCourseMapper.selectById(courseId);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程不存在");
        }
        if (!actor.roleCodes().contains("ROLE_ADMIN") && !Objects.equals(entity.getTeacherId(), actor.userId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权管理该实验课程");
        }
        return entity;
    }

    private ExperimentCourseVO requireCourseVo(Long courseId) {
        ExperimentCourseSummaryRowVO summary = experimentCourseMapper.findSummaryById(courseId);
        if (summary == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程不存在");
        }
        return hydrateCourses(List.of(summary)).get(0);
    }

    private void replaceTargets(Long courseId, ExperimentCourseSaveRequest request) {
        experimentCourseTargetClassMapper.delete(new LambdaQueryWrapper<ExperimentCourseTargetClassEntity>().eq(ExperimentCourseTargetClassEntity::getCourseId, courseId));
        experimentCourseTargetStudentMapper.delete(new LambdaQueryWrapper<ExperimentCourseTargetStudentEntity>().eq(ExperimentCourseTargetStudentEntity::getCourseId, courseId));
        for (Long classId : uniqIds(request.targetClassIds())) {
            ExperimentCourseTargetClassEntity entity = new ExperimentCourseTargetClassEntity();
            entity.setCourseId(courseId);
            entity.setClassId(classId);
            experimentCourseTargetClassMapper.insert(entity);
        }
        for (Long studentId : uniqIds(request.targetStudentIds())) {
            ExperimentCourseTargetStudentEntity entity = new ExperimentCourseTargetStudentEntity();
            entity.setCourseId(courseId);
            entity.setStudentId(studentId);
            experimentCourseTargetStudentMapper.insert(entity);
        }
    }

    private void replaceSlots(Long courseId, ExperimentCourseSaveRequest request, SemesterEntity semester) {
        Map<Long, ExperimentCourseSlotEntity> existingById = experimentCourseSlotMapper.selectList(
                        new LambdaQueryWrapper<ExperimentCourseSlotEntity>().eq(ExperimentCourseSlotEntity::getCourseId, courseId))
                .stream()
                .collect(Collectors.toMap(ExperimentCourseSlotEntity::getId, x -> x));
        Set<Long> retainedIds = new LinkedHashSet<>();
        for (int i = 0; i < request.slots().size(); i++) {
            ExperimentCourseSaveRequest.ExperimentCourseSlotRequest slotRequest = request.slots().get(i);
            ExperimentCourseSlotEntity entity;
            if (slotRequest.id() != null) {
                entity = existingById.get(slotRequest.id());
                if (entity == null) {
                    throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "场次不存在或不属于当前课程");
                }
                retainedIds.add(entity.getId());
            } else {
                entity = new ExperimentCourseSlotEntity();
                entity.setCourseId(courseId);
                entity.setCreatedAt(LocalDateTime.now());
            }
            int enrolledCount = entity.getId() == null ? 0 : nvl(experimentCourseEnrollmentMapper.countEnrolledBySlotId(entity.getId()));
            if (slotRequest.capacity() < enrolledCount) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "场次容量不能小于已报名人数");
            }
            fillSlotEntity(entity, slotRequest);
            if (entity.getId() == null) {
                experimentCourseSlotMapper.insert(entity);
                retainedIds.add(entity.getId());
            } else {
                experimentCourseSlotMapper.updateById(entity);
            }
            rebuildInstances(entity, semester, i + 1);
        }
        for (ExperimentCourseSlotEntity existing : existingById.values()) {
            if (retainedIds.contains(existing.getId())) {
                continue;
            }
            if (nvl(experimentCourseEnrollmentMapper.countEnrolledBySlotId(existing.getId())) > 0) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "已有学生选择该场次，不能直接删除");
            }
            experimentCourseSlotMapper.deleteById(existing.getId());
        }
    }
    private void fillSlotEntity(ExperimentCourseSlotEntity entity, ExperimentCourseSaveRequest.ExperimentCourseSlotRequest request) {
        String mode = upper(request.mode());
        entity.setName(blankToNull(request.name()));
        entity.setMode(mode);
        entity.setLessonDate(request.firstLessonDate());
        entity.setFirstLessonDate(request.firstLessonDate());
        entity.setSlotId(request.slotId());
        entity.setLabRoomId(request.labRoomId());
        entity.setCapacity(request.capacity());
        if ("SINGLE".equals(mode)) {
            entity.setRepeatPattern(null);
            entity.setRangeMode(null);
            entity.setRangeStartDate(request.firstLessonDate());
            entity.setRangeEndDate(request.firstLessonDate());
        } else {
            entity.setRepeatPattern(upper(request.repeatPattern()));
            entity.setRangeMode(upper(request.rangeMode()));
            entity.setRangeStartDate(request.rangeStartDate());
            entity.setRangeEndDate(request.rangeEndDate());
        }
    }

    private void rebuildInstances(ExperimentCourseSlotEntity slot, SemesterEntity semester, int slotIndex) {
        experimentCourseSlotInstanceMapper.delete(new LambdaQueryWrapper<ExperimentCourseSlotInstanceEntity>()
                .eq(ExperimentCourseSlotInstanceEntity::getSlotGroupId, slot.getId()));
        for (InstanceDraft draft : buildInstanceDrafts(slot, semester, slotIndex)) {
            ExperimentCourseSlotInstanceEntity entity = new ExperimentCourseSlotInstanceEntity();
            entity.setCourseId(slot.getCourseId());
            entity.setSlotGroupId(slot.getId());
            entity.setLessonDate(draft.lessonDate());
            entity.setTeachingWeek(draft.teachingWeek());
            entity.setDisplayName(draft.displayName());
            entity.setSlotId(slot.getSlotId());
            entity.setLabRoomId(slot.getLabRoomId());
            entity.setCapacity(slot.getCapacity());
            entity.setCreatedAt(LocalDateTime.now());
            experimentCourseSlotInstanceMapper.insert(entity);
        }
    }

    private List<InstanceDraft> buildInstanceDrafts(ExperimentCourseSlotEntity slot, SemesterEntity semester, int slotIndex) {
        String baseName = slotBaseName(slot, slotIndex);
        List<InstanceDraft> drafts = new ArrayList<>();
        if ("SINGLE".equals(upper(slot.getMode()))) {
            int week = computeTeachingWeek(slot.getFirstLessonDate(), semester);
            drafts.add(new InstanceDraft(slot.getFirstLessonDate(), week, buildInstanceName(baseName, 1)));
            return drafts;
        }
        LocalDate rangeStart = resolveRangeStart(slot, semester);
        LocalDate rangeEnd = resolveRangeEnd(slot, semester);
        long stepWeeks = "ODD_WEEK".equals(upper(slot.getRepeatPattern())) ? 2L : 1L;
        for (LocalDate lessonDate = slot.getFirstLessonDate(); !lessonDate.isAfter(rangeEnd); lessonDate = lessonDate.plusWeeks(stepWeeks)) {
            if (lessonDate.isBefore(rangeStart)) {
                continue;
            }
            int teachingWeek = computeTeachingWeek(lessonDate, semester);
            drafts.add(new InstanceDraft(lessonDate, teachingWeek, buildInstanceName(baseName, drafts.size() + 1)));
        }
        if (drafts.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前场次规则没有生成任何课次，请检查日期范围设置");
        }
        return drafts;
    }

    private List<ExperimentCourseVO> hydrateCourses(List<ExperimentCourseSummaryRowVO> summaries) {
        if (summaries.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = summaries.stream().map(ExperimentCourseSummaryRowVO::id).toList();
        Map<Long, List<ExperimentCourseSlotRowVO>> slotMap = experimentCourseSlotMapper.findRowsByCourseIds(courseIds).stream()
                .collect(Collectors.groupingBy(ExperimentCourseSlotRowVO::courseId));
        Map<Long, List<ExperimentCourseSlotInstanceRowVO>> instanceMap = experimentCourseSlotInstanceMapper.findRowsByCourseIds(courseIds).stream()
                .collect(Collectors.groupingBy(ExperimentCourseSlotInstanceRowVO::slotGroupId));
        Map<Long, List<Long>> classTargetMap = new HashMap<>();
        Map<Long, List<Long>> studentTargetMap = new HashMap<>();
        for (Long courseId : courseIds) {
            classTargetMap.put(courseId, experimentCourseTargetClassMapper.findClassIdsByCourseId(courseId));
            studentTargetMap.put(courseId, experimentCourseTargetStudentMapper.findStudentIdsByCourseId(courseId));
        }
        return summaries.stream().map(summary -> {
            ExperimentCourseVO vo = new ExperimentCourseVO();
            vo.setId(summary.id());
            vo.setTitle(summary.title());
            vo.setDescription(summary.description());
            vo.setTeacherId(summary.teacherId());
            vo.setTeacherDisplayName(summary.teacherDisplayName());
            vo.setSemesterId(summary.semesterId());
            vo.setSemesterName(summary.semesterName());
            vo.setStatus(summary.status());
            vo.setEnrollDeadlineAt(summary.enrollDeadlineAt());
            vo.setTargetClassIds(classTargetMap.getOrDefault(summary.id(), List.of()));
            vo.setTargetStudentIds(studentTargetMap.getOrDefault(summary.id(), List.of()));
            List<ExperimentCourseVO.SlotVO> slots = slotMap.getOrDefault(summary.id(), List.of()).stream()
                    .map(row -> toSlotVo(row, instanceMap.getOrDefault(row.id(), List.of())))
                    .toList();
            vo.setSlots(slots);
            vo.setTotalEnrolled(slots.stream().map(ExperimentCourseVO.SlotVO::getEnrolledCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum());
            vo.setCreatedAt(summary.createdAt());
            vo.setUpdatedAt(summary.updatedAt());
            return vo;
        }).toList();
    }

    private StudentExperimentCourseVO toStudentVo(ExperimentCourseVO course, StudentEnrollmentRowVO enrollment, boolean blocked) {
        StudentExperimentCourseVO vo = new StudentExperimentCourseVO();
        vo.setId(course.getId());
        vo.setTitle(course.getTitle());
        vo.setDescription(course.getDescription());
        vo.setTeacherId(course.getTeacherId());
        vo.setTeacherDisplayName(course.getTeacherDisplayName());
        vo.setSemesterId(course.getSemesterId());
        vo.setSemesterName(course.getSemesterName());
        vo.setStatus(course.getStatus());
        vo.setEnrollDeadlineAt(course.getEnrollDeadlineAt());
        vo.setEnrolled(enrollment != null);
        vo.setBlocked(blocked);
        vo.setBlockedReason(blocked ? "已被教师移出该实验课程，暂不可自行选课" : null);
        vo.setSelectedSlotId(enrollment == null ? null : enrollment.slotId());
        vo.setSelectedAt(enrollment == null ? null : enrollment.selectedAt());
        vo.setSlots(course.getSlots());
        return vo;
    }

    private ExperimentCourseVO.SlotVO toSlotVo(ExperimentCourseSlotRowVO row, List<ExperimentCourseSlotInstanceRowVO> instances) {
        ExperimentCourseVO.SlotVO vo = new ExperimentCourseVO.SlotVO();
        vo.setId(row.id());
        vo.setCourseId(row.courseId());
        vo.setName(row.name());
        vo.setMode(row.mode());
        vo.setFirstLessonDate(row.firstLessonDate());
        vo.setRepeatPattern(row.repeatPattern());
        vo.setRangeMode(row.rangeMode());
        vo.setRangeStartDate(row.rangeStartDate());
        vo.setRangeEndDate(row.rangeEndDate());
        vo.setSlotId(row.slotId());
        vo.setSlotCode(row.slotCode());
        vo.setSlotName(row.slotName());
        vo.setSlotStartTime(row.slotStartTime());
        vo.setSlotEndTime(row.slotEndTime());
        vo.setLabRoomId(row.labRoomId());
        vo.setLabRoomName(row.labRoomName());
        vo.setCapacity(row.capacity());
        int enrolledCount = nvl(row.enrolledCount());
        vo.setEnrolledCount(enrolledCount);
        vo.setRemainingCapacity(Math.max(0, nvl(row.capacity()) - enrolledCount));
        vo.setInstances(instances.stream().map(this::toInstanceVo).toList());
        return vo;
    }

    private ExperimentCourseVO.InstanceVO toInstanceVo(ExperimentCourseSlotInstanceRowVO row) {
        ExperimentCourseVO.InstanceVO vo = new ExperimentCourseVO.InstanceVO();
        vo.setId(row.id());
        vo.setSlotGroupId(row.slotGroupId());
        vo.setLessonDate(row.lessonDate());
        vo.setTeachingWeek(row.teachingWeek());
        vo.setDisplayName(row.displayName());
        vo.setSlotId(row.slotId());
        vo.setSlotCode(row.slotCode());
        vo.setSlotName(row.slotName());
        vo.setSlotStartTime(row.slotStartTime());
        vo.setSlotEndTime(row.slotEndTime());
        vo.setLabRoomId(row.labRoomId());
        vo.setLabRoomName(row.labRoomName());
        vo.setCapacity(row.capacity());
        return vo;
    }
    private boolean isEligible(Long courseId, SysUserEntity student) {
        boolean byClass = student.getClassId() != null && experimentCourseTargetClassMapper.findClassIdsByCourseId(courseId).contains(student.getClassId());
        boolean byStudent = experimentCourseTargetStudentMapper.findStudentIdsByCourseId(courseId).contains(student.getId());
        return byClass || byStudent;
    }

    private boolean isBlocked(Long courseId, Long studentId) {
        return experimentCourseBlockedStudentMapper.findByCourseAndStudent(courseId, studentId) != null;
    }

    private void upsertBlocked(Long courseId, Long studentId, Long teacherId) {
        ExperimentCourseBlockedStudentEntity existing = experimentCourseBlockedStudentMapper.findByCourseAndStudent(courseId, studentId);
        if (existing == null) {
            ExperimentCourseBlockedStudentEntity entity = new ExperimentCourseBlockedStudentEntity();
            entity.setCourseId(courseId);
            entity.setStudentId(studentId);
            entity.setBlockedByTeacherId(teacherId);
            entity.setBlockedAt(LocalDateTime.now());
            experimentCourseBlockedStudentMapper.insert(entity);
            return;
        }
        existing.setBlockedByTeacherId(teacherId);
        existing.setBlockedAt(LocalDateTime.now());
        experimentCourseBlockedStudentMapper.updateById(existing);
    }

    private void removeBlocked(Long courseId, Long studentId) {
        experimentCourseBlockedStudentMapper.delete(new LambdaQueryWrapper<ExperimentCourseBlockedStudentEntity>()
                .eq(ExperimentCourseBlockedStudentEntity::getCourseId, courseId)
                .eq(ExperimentCourseBlockedStudentEntity::getStudentId, studentId));
    }

    private void ensureCourseOpen(ExperimentCourseEntity course) {
        if (!"OPEN".equalsIgnoreCase(course.getStatus())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前课程未开放选课");
        }
        if (course.getEnrollDeadlineAt() != null && LocalDateTime.now().isAfter(course.getEnrollDeadlineAt())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "已超过选课截止时间");
        }
    }

    private SysUserEntity requireStudent(Long studentId) {
        SysUserEntity user = sysUserMapper.selectById(studentId);
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "学生不存在或已被禁用");
        }
        if (!sysUserMapper.findRoleCodesByUserId(studentId).contains("ROLE_STUDENT")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "当前账号不是学生");
        }
        return user;
    }

    private Map<Long, OrgClassEntity> loadClassMap(List<SysUserEntity> users) {
        List<Long> classIds = users.stream().map(SysUserEntity::getClassId).filter(Objects::nonNull).distinct().toList();
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return orgClassMapper.selectBatchIds(classIds).stream().collect(Collectors.toMap(OrgClassEntity::getId, x -> x));
    }

    private Set<Long> uniqIds(List<Long> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private LocalDate resolveRangeStart(ExperimentCourseSaveRequest.ExperimentCourseSlotRequest slot, SemesterEntity semester) {
        if ("SEMESTER".equals(upper(slot.rangeMode()))) {
            return semester.getStartDate();
        }
        if (slot.rangeStartDate() == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "自定义日期区间必须填写开始日期");
        }
        return slot.rangeStartDate();
    }

    private LocalDate resolveRangeEnd(ExperimentCourseSaveRequest.ExperimentCourseSlotRequest slot, SemesterEntity semester) {
        if ("SEMESTER".equals(upper(slot.rangeMode()))) {
            return semester.getEndDate();
        }
        if (slot.rangeEndDate() == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "自定义日期区间必须填写结束日期");
        }
        return slot.rangeEndDate();
    }

    private LocalDate resolveRangeStart(ExperimentCourseSlotEntity slot, SemesterEntity semester) {
        if ("SEMESTER".equals(upper(slot.getRangeMode()))) {
            return semester.getStartDate();
        }
        return Objects.requireNonNullElse(slot.getRangeStartDate(), semester.getStartDate());
    }

    private LocalDate resolveRangeEnd(ExperimentCourseSlotEntity slot, SemesterEntity semester) {
        if ("SEMESTER".equals(upper(slot.getRangeMode()))) {
            return semester.getEndDate();
        }
        return Objects.requireNonNullElse(slot.getRangeEndDate(), semester.getEndDate());
    }

    private int computeTeachingWeek(LocalDate lessonDate, SemesterEntity semester) {
        LocalDate semesterWeekStart = semester.getStartDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lessonWeekStart = lessonDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long diff = ChronoUnit.WEEKS.between(semesterWeekStart, lessonWeekStart);
        return Math.toIntExact(diff + 1);
    }

    private String slotBaseName(ExperimentCourseSlotEntity slot, int slotIndex) {
        String name = blankToNull(slot.getName());
        return name != null ? name : "场次" + slotIndex;
    }

    private String buildInstanceName(String baseName, int sequenceWeek) {
        return baseName + " 第" + sequenceWeek + "周";
    }

    private String blankToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean slotHasAttendanceHistory(Long slotId) {
        Long count = attendanceSessionMapper.selectCount(new LambdaQueryWrapper<AttendanceSessionEntity>()
                .eq(AttendanceSessionEntity::getExperimentCourseSlotId, slotId));
        return count != null && count > 0;
    }

    private String displayClass(OrgClassEntity clazz) {
        if (clazz == null) {
            return null;
        }
        return ClassDisplayUtils.effectiveDisplayName(clazz.getGrade(), clazz.getName());
    }

    private record InstanceDraft(LocalDate lessonDate, Integer teachingWeek, String displayName) {
    }

    public record MetaOptionVO(Long id, String name) {
    }

    public record SemesterOptionVO(Long id, String name, LocalDate startDate, LocalDate endDate) {
    }

    public record MetaVO(List<SemesterOptionVO> semesters, List<MetaOptionVO> timeSlots, List<MetaOptionVO> labRooms) {
    }
}
