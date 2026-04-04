package cn.edu.jnu.labflowreport.elective.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.util.ClassDisplayUtils;
import cn.edu.jnu.labflowreport.elective.dto.ExperimentCourseEnrollRequest;
import cn.edu.jnu.labflowreport.elective.dto.ExperimentCourseSaveRequest;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseEnrollmentRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseSlotRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseStudentOptionVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseSummaryRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseVO;
import cn.edu.jnu.labflowreport.elective.vo.StudentEnrollmentRowVO;
import cn.edu.jnu.labflowreport.elective.vo.StudentExperimentCourseVO;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseEnrollmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseSlotEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseTargetClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ExperimentCourseTargetStudentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.LabRoomEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseEnrollmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseSlotMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseTargetClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExperimentCourseTargetStudentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.LabRoomMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.schedule.mapper.TimeSlotMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

    private final ExperimentCourseMapper experimentCourseMapper;
    private final ExperimentCourseSlotMapper experimentCourseSlotMapper;
    private final ExperimentCourseTargetClassMapper experimentCourseTargetClassMapper;
    private final ExperimentCourseTargetStudentMapper experimentCourseTargetStudentMapper;
    private final ExperimentCourseEnrollmentMapper experimentCourseEnrollmentMapper;
    private final SemesterMapper semesterMapper;
    private final OrgClassMapper orgClassMapper;
    private final SysUserMapper sysUserMapper;
    private final LabRoomMapper labRoomMapper;
    private final TimeSlotMapper timeSlotMapper;

    public ExperimentCourseService(
            ExperimentCourseMapper experimentCourseMapper,
            ExperimentCourseSlotMapper experimentCourseSlotMapper,
            ExperimentCourseTargetClassMapper experimentCourseTargetClassMapper,
            ExperimentCourseTargetStudentMapper experimentCourseTargetStudentMapper,
            ExperimentCourseEnrollmentMapper experimentCourseEnrollmentMapper,
            SemesterMapper semesterMapper,
            OrgClassMapper orgClassMapper,
            SysUserMapper sysUserMapper,
            LabRoomMapper labRoomMapper,
            TimeSlotMapper timeSlotMapper
    ) {
        this.experimentCourseMapper = experimentCourseMapper;
        this.experimentCourseSlotMapper = experimentCourseSlotMapper;
        this.experimentCourseTargetClassMapper = experimentCourseTargetClassMapper;
        this.experimentCourseTargetStudentMapper = experimentCourseTargetStudentMapper;
        this.experimentCourseEnrollmentMapper = experimentCourseEnrollmentMapper;
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
    public ExperimentCourseVO createTeacherCourse(AuthenticatedUser actor, ExperimentCourseSaveRequest request) {
        validateSaveRequest(request);
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
        replaceSlots(entity.getId(), request);
        return requireCourseVo(entity.getId());
    }

    @Transactional
    public ExperimentCourseVO updateTeacherCourse(Long courseId, AuthenticatedUser actor, ExperimentCourseSaveRequest request) {
        validateSaveRequest(request);
        ExperimentCourseEntity entity = requireManageableCourse(courseId, actor);
        entity.setTitle(request.title().trim());
        entity.setDescription(blankToNull(request.description()));
        entity.setSemesterId(request.semesterId());
        entity.setEnrollDeadlineAt(request.enrollDeadlineAt());
        entity.setUpdatedAt(LocalDateTime.now());
        experimentCourseMapper.updateById(entity);
        replaceTargets(courseId, request);
        replaceSlots(courseId, request);
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

    public List<StudentExperimentCourseVO> listStudentAvailableCourses(AuthenticatedUser student) {
        SysUserEntity user = requireStudent(student.userId());
        Map<Long, StudentEnrollmentRowVO> enrolled = experimentCourseEnrollmentMapper.findActiveRowsByStudentId(student.userId()).stream()
                .collect(Collectors.toMap(StudentEnrollmentRowVO::courseId, x -> x, (a, b) -> a));
        return hydrateCourses(experimentCourseMapper.findOpenCourseSummaries(null)).stream()
                .filter(course -> isEligible(course.getId(), user))
                .map(course -> toStudentVo(course, enrolled.get(course.getId())))
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
                .toList()).stream().map(course -> toStudentVo(course, enrolled.get(course.getId()))).toList();
    }

    @Transactional
    public StudentExperimentCourseVO enroll(AuthenticatedUser student, Long courseId, ExperimentCourseEnrollRequest request) {
        SysUserEntity user = requireStudent(student.userId());
        ExperimentCourseEntity course = experimentCourseMapper.findByIdForUpdate(courseId);
        if (course == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验课程不存在");
        }
        ensureCourseOpen(course);
        if (!isEligible(courseId, user)) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "你不在该实验课程的开放范围内");
        }
        ExperimentCourseEnrollmentEntity existing = experimentCourseEnrollmentMapper.findByCourseAndStudent(courseId, student.userId());
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
        ExperimentCourseEnrollmentEntity entity = new ExperimentCourseEnrollmentEntity();
        entity.setCourseId(courseId);
        entity.setSlotId(slot.getId());
        entity.setStudentId(student.userId());
        entity.setStatus("ENROLLED");
        entity.setSelectedAt(LocalDateTime.now());
        experimentCourseEnrollmentMapper.insert(entity);
        return listStudentMyCourses(student).stream()
                .filter(item -> Objects.equals(item.getId(), courseId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "选课成功但读取失败"));
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
        List<MetaOptionVO> semesters = semesterMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SemesterEntity::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> new MetaOptionVO(item.getId(), item.getName()))
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

    private void validateSaveRequest(ExperimentCourseSaveRequest request) {
        if (semesterMapper.selectById(request.semesterId()) == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "semesterId 不存在");
        }
        Set<Long> classIds = uniqIds(request.targetClassIds());
        if (!classIds.isEmpty() && orgClassMapper.selectBatchIds(classIds).size() != classIds.size()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "开放班级中存在无效班级");
        }
        Set<Long> studentIds = uniqIds(request.targetStudentIds());
        if (!studentIds.isEmpty() && sysUserMapper.selectBatchIds(studentIds).size() != studentIds.size()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "指定学生中存在无效用户");
        }
        request.slots().forEach(slot -> {
            if (timeSlotMapper.selectById(slot.slotId()) == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "存在无效节次");
            }
            if (labRoomMapper.selectById(slot.labRoomId()) == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "存在无效实验室");
            }
        });
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

    private void replaceSlots(Long courseId, ExperimentCourseSaveRequest request) {
        experimentCourseSlotMapper.delete(new LambdaQueryWrapper<ExperimentCourseSlotEntity>().eq(ExperimentCourseSlotEntity::getCourseId, courseId));
        request.slots().forEach(slot -> {
            ExperimentCourseSlotEntity entity = new ExperimentCourseSlotEntity();
            entity.setCourseId(courseId);
            entity.setLessonDate(slot.lessonDate());
            entity.setSlotId(slot.slotId());
            entity.setLabRoomId(slot.labRoomId());
            entity.setCapacity(slot.capacity());
            entity.setCreatedAt(LocalDateTime.now());
            experimentCourseSlotMapper.insert(entity);
        });
    }

    private List<ExperimentCourseVO> hydrateCourses(List<ExperimentCourseSummaryRowVO> summaries) {
        if (summaries.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = summaries.stream().map(ExperimentCourseSummaryRowVO::id).toList();
        Map<Long, List<ExperimentCourseSlotRowVO>> slotMap = experimentCourseSlotMapper.findRowsByCourseIds(courseIds).stream()
                .collect(Collectors.groupingBy(ExperimentCourseSlotRowVO::courseId));
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
            List<ExperimentCourseVO.SlotVO> slots = slotMap.getOrDefault(summary.id(), List.of()).stream().map(this::toSlotVo).toList();
            vo.setSlots(slots);
            vo.setTotalEnrolled(slots.stream().map(ExperimentCourseVO.SlotVO::getEnrolledCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum());
            vo.setCreatedAt(summary.createdAt());
            vo.setUpdatedAt(summary.updatedAt());
            return vo;
        }).toList();
    }

    private StudentExperimentCourseVO toStudentVo(ExperimentCourseVO course, StudentEnrollmentRowVO enrollment) {
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
        vo.setSelectedSlotId(enrollment == null ? null : enrollment.slotId());
        vo.setSelectedAt(enrollment == null ? null : enrollment.selectedAt());
        vo.setSlots(course.getSlots());
        return vo;
    }

    private ExperimentCourseVO.SlotVO toSlotVo(ExperimentCourseSlotRowVO row) {
        ExperimentCourseVO.SlotVO vo = new ExperimentCourseVO.SlotVO();
        vo.setId(row.id());
        vo.setCourseId(row.courseId());
        vo.setLessonDate(row.lessonDate());
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
        return vo;
    }

    private boolean isEligible(Long courseId, SysUserEntity student) {
        boolean byClass = student.getClassId() != null && experimentCourseTargetClassMapper.findClassIdsByCourseId(courseId).contains(student.getClassId());
        boolean byStudent = experimentCourseTargetStudentMapper.findStudentIdsByCourseId(courseId).contains(student.getId());
        return byClass || byStudent;
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

    private String displayClass(OrgClassEntity clazz) {
        if (clazz == null) {
            return null;
        }
        return ClassDisplayUtils.effectiveDisplayName(clazz.getGrade(), clazz.getName());
    }

    public record MetaOptionVO(Long id, String name) {
    }

    public record MetaVO(List<MetaOptionVO> semesters, List<MetaOptionVO> timeSlots, List<MetaOptionVO> labRooms) {
    }
}
