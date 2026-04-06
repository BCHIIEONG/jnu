package cn.edu.jnu.labflowreport.statistics.service;

import cn.edu.jnu.labflowreport.admin.service.AdminAuditActions;
import cn.edu.jnu.labflowreport.admin.service.AdminAuditService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.ExportRecordEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExportRecordMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.statistics.vo.AdminStatisticsDashboardVO;
import cn.edu.jnu.labflowreport.statistics.vo.StatisticsChartVO;
import cn.edu.jnu.labflowreport.statistics.vo.StatisticsOptionVO;
import cn.edu.jnu.labflowreport.statistics.vo.TeacherStatisticsDashboardVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SemesterMapper semesterMapper;
    private final ExportRecordMapper exportRecordMapper;
    private final ObjectMapper objectMapper;
    private final AdminAuditService adminAuditService;

    public StatisticsService(
            NamedParameterJdbcTemplate jdbcTemplate,
            SemesterMapper semesterMapper,
            ExportRecordMapper exportRecordMapper,
            ObjectMapper objectMapper,
            AdminAuditService adminAuditService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.semesterMapper = semesterMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.objectMapper = objectMapper;
        this.adminAuditService = adminAuditService;
    }

    public TeacherStatisticsDashboardVO getTeacherDashboard(
            AuthenticatedUser teacher,
            Long semesterId,
            LocalDate from,
            LocalDate to
    ) {
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        List<TeacherStatisticsDashboardVO.SemesterOption> semesterOptions = loadSemesterOptions();
        List<TaskMetaRow> tasks = loadTasks(teacher.userId(), null, filters, false);
        List<SubmissionRow> submissions = loadSubmissions(teacher.userId(), null, filters);
        List<ReviewRow> reviews = loadReviews(teacher.userId(), null, filters);
        List<CompletionRow> completions = loadCompletions(teacher.userId(), null, filters);
        List<DeviceRequestRow> deviceRequests = loadDeviceRequests(teacher.userId(), null, filters);
        List<SessionAggregateRow> sessionRows = loadAttendanceSessionAggregates(teacher.userId(), null, filters);
        List<CourseMetaRow> courses = loadCourses(teacher.userId(), null, filters);
        Map<Long, Long> activeEnrollmentByCourse = loadActiveEnrollmentCounts(teacher.userId(), null, filters);

        Map<Long, TeacherStatisticsDashboardVO.TaskRow> taskTable = buildTeacherTaskTable(tasks, submissions, reviews, completions);
        Map<Long, Long> courseAttendanceCount = sessionRows.stream()
                .filter(row -> row.experimentCourseId() != null)
                .collect(Collectors.groupingBy(
                        SessionAggregateRow::experimentCourseId,
                        Collectors.mapping(
                                SessionAggregateRow::sessionId,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size())
                        )));
        List<TeacherStatisticsDashboardVO.ExperimentCourseRow> courseTable = courses.stream()
                .map(course -> new TeacherStatisticsDashboardVO.ExperimentCourseRow(
                        course.id(),
                        course.title(),
                        course.slotCount(),
                        activeEnrollmentByCourse.getOrDefault(course.id(), 0L),
                        courseAttendanceCount.getOrDefault(course.id(), 0L)))
                .sorted(Comparator.comparing(TeacherStatisticsDashboardVO.ExperimentCourseRow::courseTitle, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<TeacherStatisticsDashboardVO.DeviceRequestRow> deviceTable = buildTeacherDeviceTable(tasks, deviceRequests);

        TeacherStatisticsDashboardVO.Summary summary = new TeacherStatisticsDashboardVO.Summary(
                tasks.size(),
                tasks.stream().filter(task -> "OPEN".equalsIgnoreCase(task.status())).count(),
                submissions.size(),
                reviews.size(),
                averageScore(reviews.stream().map(ReviewRow::score).toList()),
                completions.size(),
                sessionRows.stream().map(SessionAggregateRow::sessionId).distinct().count(),
                weightedRate(sessionRows.stream().mapToLong(SessionAggregateRow::checkedInCount).sum(), sessionRows.stream().mapToLong(SessionAggregateRow::totalCount).sum()),
                courses.size(),
                activeEnrollmentByCourse.values().stream().mapToLong(Long::longValue).sum(),
                deviceRequests.stream().filter(row -> "PENDING".equalsIgnoreCase(row.status())).count()
        );

        TeacherStatisticsDashboardVO.Charts charts = new TeacherStatisticsDashboardVO.Charts(
                buildTaskTrend(submissions, reviews, filters),
                buildAttendanceTrend(sessionRows, filters)
        );

        return new TeacherStatisticsDashboardVO(
                new TeacherStatisticsDashboardVO.Filters(
                        filters.semester().getId(),
                        filters.semester().getName(),
                        filters.from(),
                        filters.to(),
                        semesterOptions
                ),
                summary,
                charts,
                new TeacherStatisticsDashboardVO.Tables(
                        taskTable.values().stream()
                                .sorted(Comparator.comparing(TeacherStatisticsDashboardVO.TaskRow::taskTitle, Comparator.nullsLast(String::compareTo)))
                                .toList(),
                        courseTable,
                        deviceTable
                )
        );
    }

    public AdminStatisticsDashboardVO getAdminDashboard(
            AuthenticatedUser admin,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId
    ) {
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        List<TaskMetaRow> tasks = loadTasks(teacherId, classId, filters, true);
        List<SubmissionRow> submissions = loadSubmissions(teacherId, classId, filters);
        List<ReviewRow> reviews = loadReviews(teacherId, classId, filters);
        List<CompletionRow> completions = loadCompletions(teacherId, classId, filters);
        List<DeviceRequestRow> deviceRequests = loadDeviceRequests(teacherId, classId, filters);
        List<SessionAggregateRow> sessionRows = loadAttendanceSessionAggregates(teacherId, classId, filters);
        List<SessionClassAggregateRow> classAttendanceRows = loadAttendanceClassAggregates(teacherId, classId, filters);
        List<CourseMetaRow> courses = loadCourses(teacherId, classId, filters);
        Map<Long, Long> activeEnrollmentByCourse = loadActiveEnrollmentCounts(teacherId, classId, filters);
        Map<Long, Long> studentCountByClass = loadStudentCountsByClass(classId);
        Map<Long, String> teacherNames = loadTeacherNames();
        Map<Long, String> classNames = loadClassNames();

        Map<Long, AdminStatisticsDashboardVO.TeacherRow> teacherTable = buildAdminTeacherTable(tasks, submissions, reviews, sessionRows, teacherNames);
        List<AdminStatisticsDashboardVO.ClassRow> classTable = buildAdminClassTable(submissions, classAttendanceRows, studentCountByClass, classNames);
        Map<Long, Long> courseAttendanceCount = sessionRows.stream()
                .filter(row -> row.experimentCourseId() != null)
                .collect(Collectors.groupingBy(
                        SessionAggregateRow::experimentCourseId,
                        Collectors.mapping(
                                SessionAggregateRow::sessionId,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size())
                        )));
        List<AdminStatisticsDashboardVO.ExperimentCourseRow> courseTable = courses.stream()
                .map(course -> new AdminStatisticsDashboardVO.ExperimentCourseRow(
                        course.id(),
                        course.title(),
                        course.teacherId(),
                        course.teacherName(),
                        activeEnrollmentByCourse.getOrDefault(course.id(), 0L),
                        course.slotCount(),
                        courseAttendanceCount.getOrDefault(course.id(), 0L)))
                .sorted(Comparator.comparing(AdminStatisticsDashboardVO.ExperimentCourseRow::courseTitle, Comparator.nullsLast(String::compareTo)))
                .toList();

        long teacherCount = resolveTeacherCount(teacherId, classId, tasks, courses, sessionRows, teacherNames.keySet());
        long classCount = resolveClassCount(teacherId, classId, submissions, classAttendanceRows, classNames.keySet());
        long studentCount = resolveStudentCount(teacherId, classId, filters, studentCountByClass);

        return new AdminStatisticsDashboardVO(
                new AdminStatisticsDashboardVO.Filters(
                        filters.semester().getId(),
                        filters.semester().getName(),
                        filters.from(),
                        filters.to(),
                        teacherId,
                        classId,
                        loadAdminSemesterOptions(),
                        loadTeacherOptions(),
                        loadClassOptions()
                ),
                new AdminStatisticsDashboardVO.Summary(
                        teacherCount,
                        studentCount,
                        classCount,
                        tasks.size(),
                        courses.size(),
                        submissions.size(),
                        reviews.size(),
                        averageScore(reviews.stream().map(ReviewRow::score).toList()),
                        sessionRows.stream().map(SessionAggregateRow::sessionId).distinct().count(),
                        weightedRate(sessionRows.stream().mapToLong(SessionAggregateRow::checkedInCount).sum(), sessionRows.stream().mapToLong(SessionAggregateRow::totalCount).sum()),
                        activeEnrollmentByCourse.values().stream().mapToLong(Long::longValue).sum(),
                        deviceRequests.stream().filter(row -> "PENDING".equalsIgnoreCase(row.status())).count()
                ),
                new AdminStatisticsDashboardVO.Charts(
                        buildTaskTrend(submissions, reviews, filters),
                        buildAttendanceTrend(sessionRows, filters)
                ),
                new AdminStatisticsDashboardVO.Tables(
                        teacherTable.values().stream()
                                .sorted(Comparator.comparing(AdminStatisticsDashboardVO.TeacherRow::teacherName, Comparator.nullsLast(String::compareTo)))
                                .toList(),
                        classTable,
                        courseTable
                )
        );
    }

    public String exportTeacherTaskStatsCsv(AuthenticatedUser teacher, Long semesterId, LocalDate from, LocalDate to) {
        TeacherStatisticsDashboardVO dashboard = getTeacherDashboard(teacher, semesterId, from, to);
        recordTeacherExport(teacher, "TEACHER_TASK_STATS", Map.of(
                "semesterId", dashboard.filters().semesterId(),
                "from", String.valueOf(dashboard.filters().from()),
                "to", String.valueOf(dashboard.filters().to())
        ));
        StringBuilder csv = new StringBuilder();
        csv.append("taskId,taskTitle,submissionCount,reviewedSubmissionCount,avgScore,confirmedCompletionCount\n");
        for (TeacherStatisticsDashboardVO.TaskRow row : dashboard.tables().taskTable()) {
            csv.append(cell(row.taskId())).append(",")
                    .append(cell(row.taskTitle())).append(",")
                    .append(cell(row.submissionCount())).append(",")
                    .append(cell(row.reviewedSubmissionCount())).append(",")
                    .append(cell(row.avgScore())).append(",")
                    .append(cell(row.confirmedCompletionCount())).append("\n");
        }
        return csv.toString();
    }

    public String exportTeacherExperimentCourseStatsCsv(AuthenticatedUser teacher, Long semesterId, LocalDate from, LocalDate to) {
        TeacherStatisticsDashboardVO dashboard = getTeacherDashboard(teacher, semesterId, from, to);
        recordTeacherExport(teacher, "TEACHER_EXPERIMENT_COURSE_STATS", Map.of(
                "semesterId", dashboard.filters().semesterId(),
                "from", String.valueOf(dashboard.filters().from()),
                "to", String.valueOf(dashboard.filters().to())
        ));
        StringBuilder csv = new StringBuilder();
        csv.append("courseId,courseTitle,slotCount,activeEnrollmentCount,attendanceSessionCount\n");
        for (TeacherStatisticsDashboardVO.ExperimentCourseRow row : dashboard.tables().experimentCourseTable()) {
            csv.append(cell(row.courseId())).append(",")
                    .append(cell(row.courseTitle())).append(",")
                    .append(cell(row.slotCount())).append(",")
                    .append(cell(row.activeEnrollmentCount())).append(",")
                    .append(cell(row.attendanceSessionCount())).append("\n");
        }
        return csv.toString();
    }

    public String exportTeacherDeviceRequestStatsCsv(AuthenticatedUser teacher, Long semesterId, LocalDate from, LocalDate to) {
        TeacherStatisticsDashboardVO dashboard = getTeacherDashboard(teacher, semesterId, from, to);
        recordTeacherExport(teacher, "TEACHER_DEVICE_REQUEST_STATS", Map.of(
                "semesterId", dashboard.filters().semesterId(),
                "from", String.valueOf(dashboard.filters().from()),
                "to", String.valueOf(dashboard.filters().to())
        ));
        StringBuilder csv = new StringBuilder();
        csv.append("taskId,taskTitle,pendingCount,approvedCount,borrowedCount,returnedCount\n");
        for (TeacherStatisticsDashboardVO.DeviceRequestRow row : dashboard.tables().deviceRequestTable()) {
            csv.append(cell(row.taskId())).append(",")
                    .append(cell(row.taskTitle())).append(",")
                    .append(cell(row.pendingCount())).append(",")
                    .append(cell(row.approvedCount())).append(",")
                    .append(cell(row.borrowedCount())).append(",")
                    .append(cell(row.returnedCount())).append("\n");
        }
        return csv.toString();
    }

    public String exportAdminTeacherStatsCsv(
            AuthenticatedUser admin,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId
    ) {
        AdminStatisticsDashboardVO dashboard = getAdminDashboard(admin, semesterId, from, to, teacherId, classId);
        recordAdminStatisticsExport(admin, "teachers", dashboard.filters());
        StringBuilder csv = new StringBuilder();
        csv.append("teacherId,teacherName,taskCount,submissionCount,reviewedSubmissionCount,attendanceSessionCount,avgAttendanceRate\n");
        for (AdminStatisticsDashboardVO.TeacherRow row : dashboard.tables().teacherTable()) {
            csv.append(cell(row.teacherId())).append(",")
                    .append(cell(row.teacherName())).append(",")
                    .append(cell(row.taskCount())).append(",")
                    .append(cell(row.submissionCount())).append(",")
                    .append(cell(row.reviewedSubmissionCount())).append(",")
                    .append(cell(row.attendanceSessionCount())).append(",")
                    .append(cell(row.avgAttendanceRate())).append("\n");
        }
        return csv.toString();
    }

    public String exportAdminClassStatsCsv(
            AuthenticatedUser admin,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId
    ) {
        AdminStatisticsDashboardVO dashboard = getAdminDashboard(admin, semesterId, from, to, teacherId, classId);
        recordAdminStatisticsExport(admin, "classes", dashboard.filters());
        StringBuilder csv = new StringBuilder();
        csv.append("classId,className,studentCount,submissionCount,attendanceSessionCount,avgAttendanceRate\n");
        for (AdminStatisticsDashboardVO.ClassRow row : dashboard.tables().classTable()) {
            csv.append(cell(row.classId())).append(",")
                    .append(cell(row.className())).append(",")
                    .append(cell(row.studentCount())).append(",")
                    .append(cell(row.submissionCount())).append(",")
                    .append(cell(row.attendanceSessionCount())).append(",")
                    .append(cell(row.avgAttendanceRate())).append("\n");
        }
        return csv.toString();
    }

    public String exportAdminExperimentCourseStatsCsv(
            AuthenticatedUser admin,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId
    ) {
        AdminStatisticsDashboardVO dashboard = getAdminDashboard(admin, semesterId, from, to, teacherId, classId);
        recordAdminStatisticsExport(admin, "experiment_courses", dashboard.filters());
        StringBuilder csv = new StringBuilder();
        csv.append("courseId,courseTitle,teacherId,teacherName,activeEnrollmentCount,slotCount,attendanceSessionCount\n");
        for (AdminStatisticsDashboardVO.ExperimentCourseRow row : dashboard.tables().experimentCourseTable()) {
            csv.append(cell(row.courseId())).append(",")
                    .append(cell(row.courseTitle())).append(",")
                    .append(cell(row.teacherId())).append(",")
                    .append(cell(row.teacherName())).append(",")
                    .append(cell(row.activeEnrollmentCount())).append(",")
                    .append(cell(row.slotCount())).append(",")
                    .append(cell(row.attendanceSessionCount())).append("\n");
        }
        return csv.toString();
    }

    private ResolvedFilters resolveFilters(Long semesterId, LocalDate from, LocalDate to) {
        List<SemesterEntity> semesters = semesterMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SemesterEntity::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SemesterEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (semesters.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "请先配置学期");
        }
        LocalDate today = LocalDate.now();
        SemesterEntity semester;
        if (semesterId != null) {
            semester = semesters.stream().filter(item -> Objects.equals(item.getId(), semesterId)).findFirst().orElse(null);
            if (semester == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "学期不存在");
            }
        } else {
            semester = semesters.stream()
                    .filter(item -> item.getStartDate() != null && item.getEndDate() != null
                            && !today.isBefore(item.getStartDate()) && !today.isAfter(item.getEndDate()))
                    .findFirst()
                    .orElse(semesters.get(0));
        }
        LocalDate resolvedFrom = from != null ? from : semester.getStartDate();
        LocalDate resolvedTo = to != null ? to : semester.getEndDate();
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "起始日期不能晚于结束日期");
        }
        return new ResolvedFilters(semester, resolvedFrom, resolvedTo);
    }

    private List<TeacherStatisticsDashboardVO.SemesterOption> loadSemesterOptions() {
        return semesterMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SemesterEntity::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> new TeacherStatisticsDashboardVO.SemesterOption(item.getId(), item.getName(), item.getStartDate(), item.getEndDate()))
                .toList();
    }

    private List<AdminStatisticsDashboardVO.SemesterOption> loadAdminSemesterOptions() {
        return semesterMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SemesterEntity::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> new AdminStatisticsDashboardVO.SemesterOption(item.getId(), item.getName(), item.getStartDate(), item.getEndDate()))
                .toList();
    }

    private List<StatisticsOptionVO> loadTeacherOptions() {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT su.id AS id, COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS label
                FROM sys_user su
                JOIN sys_user_role ur ON ur.user_id = su.id
                JOIN sys_role sr ON sr.id = ur.role_id
                WHERE su.enabled = TRUE
                  AND sr.code = 'ROLE_TEACHER'
                ORDER BY label ASC
                """, new MapSqlParameterSource()).stream()
                .map(row -> new StatisticsOptionVO(longVal(row.get("id")), str(row.get("label"))))
                .toList();
    }

    private List<StatisticsOptionVO> loadClassOptions() {
        return jdbcTemplate.queryForList("""
                SELECT c.id AS id,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS label
                FROM org_class c
                LEFT JOIN org_department d ON d.id = c.department_id
                ORDER BY c.grade DESC, c.id DESC
                """, new MapSqlParameterSource()).stream()
                .map(row -> new StatisticsOptionVO(longVal(row.get("id")), str(row.get("label"))))
                .toList();
    }

    private Map<Long, String> loadTeacherNames() {
        return loadTeacherOptions().stream().collect(Collectors.toMap(StatisticsOptionVO::id, StatisticsOptionVO::label));
    }

    private Map<Long, String> loadClassNames() {
        return loadClassOptions().stream().collect(Collectors.toMap(StatisticsOptionVO::id, StatisticsOptionVO::label));
    }

    private List<TaskMetaRow> loadTasks(Long teacherId, Long classId, ResolvedFilters filters, boolean adminView) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT t.id,
                       t.title,
                       t.status,
                       t.publisher_id,
                       COALESCE(NULLIF(TRIM(u.display_name), ''), u.username) AS publisher_name,
                       t.experiment_course_id,
                       t.created_at
                FROM exp_task t
                JOIN sys_user u ON u.id = t.publisher_id
                LEFT JOIN experiment_course ec ON ec.id = t.experiment_course_id
                WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("semesterId", filters.semester().getId())
                .addValue("fromDate", Date.valueOf(filters.from()))
                .addValue("toDate", Date.valueOf(filters.to()))
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND t.publisher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        sql.append("""
                 AND (
                    (t.experiment_course_id IS NOT NULL AND ec.semester_id = :semesterId)
                    OR (t.experiment_course_id IS NULL AND CAST(t.created_at AS DATE) BETWEEN :fromDate AND :toDate)
                 )
                """);
        if (adminView && classId != null) {
            sql.append("""
                     AND (
                        EXISTS (SELECT 1 FROM exp_task_target_class tc WHERE tc.task_id = t.id AND tc.class_id = :classId)
                        OR EXISTS (
                            SELECT 1 FROM report_submission rs
                            JOIN sys_user su ON su.id = rs.student_id
                            WHERE rs.task_id = t.id
                              AND su.class_id = :classId
                              AND rs.submitted_at BETWEEN :fromTime AND :toTime
                        )
                        OR EXISTS (
                            SELECT 1 FROM task_completion tc
                            JOIN sys_user su ON su.id = tc.student_id
                            WHERE tc.task_id = t.id
                              AND tc.status = 'CONFIRMED'
                              AND su.class_id = :classId
                              AND tc.confirmed_at BETWEEN :fromTime AND :toTime
                        )
                        OR EXISTS (
                            SELECT 1 FROM task_device_request dr
                            JOIN sys_user su ON su.id = dr.student_id
                            WHERE dr.task_id = t.id
                              AND su.class_id = :classId
                              AND dr.created_at BETWEEN :fromTime AND :toTime
                        )
                        OR EXISTS (
                            SELECT 1
                            FROM experiment_course_enrollment e
                            JOIN sys_user su ON su.id = e.student_id
                            WHERE e.course_id = t.experiment_course_id
                              AND e.status = 'ENROLLED'
                              AND su.class_id = :classId
                        )
                     )
                    """);
            params.addValue("classId", classId);
        }
        sql.append(" ORDER BY t.created_at DESC, t.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new TaskMetaRow(
                        longVal(row.get("id")),
                        str(row.get("title")),
                        str(row.get("status")),
                        longVal(row.get("publisher_id")),
                        str(row.get("publisher_name")),
                        nullableLong(row.get("experiment_course_id")),
                        time(row.get("created_at"))
                ))
                .toList();
    }

    private List<SubmissionRow> loadSubmissions(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT rs.task_id,
                       t.publisher_id,
                       rs.student_id,
                       su.class_id,
                       CAST(rs.submitted_at AS DATE) AS stat_day
                FROM report_submission rs
                JOIN exp_task t ON t.id = rs.task_id
                JOIN sys_user su ON su.id = rs.student_id
                WHERE rs.submitted_at BETWEEN :fromTime AND :toTime
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND t.publisher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        }
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new SubmissionRow(
                        longVal(row.get("task_id")),
                        longVal(row.get("publisher_id")),
                        longVal(row.get("student_id")),
                        nullableLong(row.get("class_id")),
                        date(row.get("stat_day"))
                ))
                .toList();
    }

    private List<ReviewRow> loadReviews(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT rs.task_id,
                       t.publisher_id,
                       rs.student_id,
                       su.class_id,
                       CAST(rr.reviewed_at AS DATE) AS stat_day,
                       rr.score
                FROM report_review rr
                JOIN report_submission rs ON rs.id = rr.submission_id
                JOIN exp_task t ON t.id = rs.task_id
                JOIN sys_user su ON su.id = rs.student_id
                WHERE rr.reviewed_at BETWEEN :fromTime AND :toTime
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND t.publisher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        }
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new ReviewRow(
                        longVal(row.get("task_id")),
                        longVal(row.get("publisher_id")),
                        longVal(row.get("student_id")),
                        nullableLong(row.get("class_id")),
                        date(row.get("stat_day")),
                        decimal(row.get("score"))
                ))
                .toList();
    }

    private List<CompletionRow> loadCompletions(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT tc.task_id,
                       t.publisher_id,
                       tc.student_id,
                       su.class_id
                FROM task_completion tc
                JOIN exp_task t ON t.id = tc.task_id
                JOIN sys_user su ON su.id = tc.student_id
                WHERE tc.status = 'CONFIRMED'
                  AND tc.confirmed_at BETWEEN :fromTime AND :toTime
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND t.publisher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        }
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new CompletionRow(
                        longVal(row.get("task_id")),
                        longVal(row.get("publisher_id")),
                        longVal(row.get("student_id")),
                        nullableLong(row.get("class_id"))
                ))
                .toList();
    }

    private List<DeviceRequestRow> loadDeviceRequests(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT dr.task_id,
                       t.publisher_id,
                       dr.student_id,
                       su.class_id,
                       dr.status
                FROM task_device_request dr
                JOIN exp_task t ON t.id = dr.task_id
                JOIN sys_user su ON su.id = dr.student_id
                WHERE dr.created_at BETWEEN :fromTime AND :toTime
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND t.publisher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        }
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new DeviceRequestRow(
                        longVal(row.get("task_id")),
                        longVal(row.get("publisher_id")),
                        longVal(row.get("student_id")),
                        nullableLong(row.get("class_id")),
                        str(row.get("status"))
                ))
                .toList();
    }

    private List<CourseMetaRow> loadCourses(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT ec.id,
                       ec.title,
                       ec.teacher_id,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS teacher_name,
                       COUNT(DISTINCT ecs.id) AS slot_count
                FROM experiment_course ec
                JOIN sys_user su ON su.id = ec.teacher_id
                LEFT JOIN experiment_course_slot ecs ON ecs.course_id = ec.id
                WHERE ec.semester_id = :semesterId
                """);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("semesterId", filters.semester().getId());
        if (teacherId != null) {
            sql.append(" AND ec.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append("""
                     AND EXISTS (
                        SELECT 1
                        FROM experiment_course_enrollment e
                        JOIN sys_user su2 ON su2.id = e.student_id
                        WHERE e.course_id = ec.id
                          AND e.status = 'ENROLLED'
                          AND su2.class_id = :classId
                     )
                    """);
            params.addValue("classId", classId);
        }
        sql.append(" GROUP BY ec.id, ec.title, ec.teacher_id, teacher_name ORDER BY ec.created_at DESC, ec.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new CourseMetaRow(
                        longVal(row.get("id")),
                        str(row.get("title")),
                        longVal(row.get("teacher_id")),
                        str(row.get("teacher_name")),
                        longVal(row.get("slot_count"))
                ))
                .toList();
    }

    private Map<Long, Long> loadActiveEnrollmentCounts(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.course_id, COUNT(*) AS active_count
                FROM experiment_course_enrollment e
                JOIN experiment_course ec ON ec.id = e.course_id
                JOIN sys_user su ON su.id = e.student_id
                WHERE e.status = 'ENROLLED'
                  AND ec.semester_id = :semesterId
                """);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("semesterId", filters.semester().getId());
        if (teacherId != null) {
            sql.append(" AND ec.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        }
        sql.append(" GROUP BY e.course_id");
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .collect(Collectors.toMap(row -> longVal(row.get("course_id")), row -> longVal(row.get("active_count"))));
    }

    private Map<Long, Long> loadStudentCountsByClass(Long classId) {
        StringBuilder sql = new StringBuilder("""
                SELECT su.class_id, COUNT(*) AS total_count
                FROM sys_user su
                JOIN sys_user_role ur ON ur.user_id = su.id
                JOIN sys_role sr ON sr.id = ur.role_id
                WHERE su.enabled = TRUE
                  AND su.class_id IS NOT NULL
                  AND sr.code = 'ROLE_STUDENT'
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        }
        sql.append(" GROUP BY su.class_id");
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .collect(Collectors.toMap(row -> longVal(row.get("class_id")), row -> longVal(row.get("total_count"))));
    }

    private List<SessionAggregateRow> loadAttendanceSessionAggregates(Long teacherId, Long classId, ResolvedFilters filters) {
        String checkedInSql = """
                SELECT ar.session_id, COUNT(DISTINCT ar.student_id) AS checked_in_count
                FROM attendance_record ar
                %s
                GROUP BY ar.session_id
                """.formatted(classId == null ? "" : "JOIN sys_user su_rec ON su_rec.id = ar.student_id WHERE su_rec.class_id = :classId");
        String enrolledSql = """
                SELECT e.slot_id, COUNT(*) AS total_count
                FROM experiment_course_enrollment e
                %s
                WHERE e.status = 'ENROLLED'
                GROUP BY e.slot_id
                """.formatted(classId == null ? "" : "JOIN sys_user su_enroll ON su_enroll.id = e.student_id AND su_enroll.class_id = :classId");
        String classRosterSql = """
                SELECT su.class_id, COUNT(*) AS total_count
                FROM sys_user su
                JOIN sys_user_role ur ON ur.user_id = su.id
                JOIN sys_role sr ON sr.id = ur.role_id
                WHERE su.enabled = TRUE
                  AND su.class_id IS NOT NULL
                  AND sr.code = 'ROLE_STUDENT'
                  %s
                GROUP BY su.class_id
                """.formatted(classId == null ? "" : "AND su.class_id = :classId");
        StringBuilder sql = new StringBuilder("""
                SELECT s.id AS session_id,
                       s.teacher_id,
                       s.experiment_course_id,
                       CAST(s.started_at AS DATE) AS stat_day,
                       COALESCE(rec.checked_in_count, 0) AS checked_in_count,
                       CASE
                         WHEN s.source_type = 'EXPERIMENT_COURSE' THEN COALESCE(exp.total_count, 0)
                         ELSE COALESCE(cls.total_count, 0)
                       END AS total_count
                FROM attendance_session s
                LEFT JOIN (""" + checkedInSql + ") rec ON rec.session_id = s.id\n" +
                "LEFT JOIN (" + enrolledSql + ") exp ON exp.slot_id = s.experiment_course_slot_id\n" +
                "LEFT JOIN (" + classRosterSql + ") cls ON cls.class_id = s.class_id\n" +
                "WHERE s.started_at BETWEEN :fromTime AND :toTime");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND s.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append("""
                     AND (
                        (s.source_type = 'CLASS_SCHEDULE' AND s.class_id = :classId)
                        OR (
                           s.source_type = 'EXPERIMENT_COURSE'
                           AND EXISTS (
                               SELECT 1
                               FROM experiment_course_enrollment e2
                               JOIN sys_user su2 ON su2.id = e2.student_id
                               WHERE e2.slot_id = s.experiment_course_slot_id
                                 AND e2.status = 'ENROLLED'
                                 AND su2.class_id = :classId
                           )
                        )
                     )
                    """);
            params.addValue("classId", classId);
        }
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new SessionAggregateRow(
                        longVal(row.get("session_id")),
                        longVal(row.get("teacher_id")),
                        nullableLong(row.get("experiment_course_id")),
                        date(row.get("stat_day")),
                        longVal(row.get("checked_in_count")),
                        longVal(row.get("total_count"))
                ))
                .toList();
    }

    private List<SessionClassAggregateRow> loadAttendanceClassAggregates(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT base.session_id,
                       base.teacher_id,
                       base.class_id,
                       base.stat_day,
                       base.checked_in_count,
                       base.total_count
                FROM (
                    SELECT s.id AS session_id,
                           s.teacher_id,
                           s.class_id,
                           CAST(s.started_at AS DATE) AS stat_day,
                           COALESCE(rec.checked_in_count, 0) AS checked_in_count,
                           COALESCE(cls.total_count, 0) AS total_count
                    FROM attendance_session s
                    LEFT JOIN (
                        SELECT ar.session_id, COUNT(DISTINCT ar.student_id) AS checked_in_count
                        FROM attendance_record ar
                        GROUP BY ar.session_id
                    ) rec ON rec.session_id = s.id
                    LEFT JOIN (
                        SELECT su.class_id, COUNT(*) AS total_count
                        FROM sys_user su
                        JOIN sys_user_role ur ON ur.user_id = su.id
                        JOIN sys_role sr ON sr.id = ur.role_id
                        WHERE su.enabled = TRUE
                          AND su.class_id IS NOT NULL
                          AND sr.code = 'ROLE_STUDENT'
                        GROUP BY su.class_id
                    ) cls ON cls.class_id = s.class_id
                    WHERE s.source_type = 'CLASS_SCHEDULE'
                      AND s.started_at BETWEEN :fromTime AND :toTime

                    UNION ALL

                    SELECT s.id AS session_id,
                           s.teacher_id,
                           su.class_id,
                           CAST(s.started_at AS DATE) AS stat_day,
                           COUNT(DISTINCT ar.student_id) AS checked_in_count,
                           COUNT(DISTINCT e.student_id) AS total_count
                    FROM attendance_session s
                    JOIN experiment_course_enrollment e
                      ON e.slot_id = s.experiment_course_slot_id
                     AND e.status = 'ENROLLED'
                    JOIN sys_user su ON su.id = e.student_id
                    LEFT JOIN attendance_record ar
                      ON ar.session_id = s.id
                     AND ar.student_id = e.student_id
                    WHERE s.source_type = 'EXPERIMENT_COURSE'
                      AND s.started_at BETWEEN :fromTime AND :toTime
                    GROUP BY s.id, s.teacher_id, su.class_id, CAST(s.started_at AS DATE)
                ) base
                WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND base.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append(" AND base.class_id = :classId");
            params.addValue("classId", classId);
        }
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new SessionClassAggregateRow(
                        longVal(row.get("session_id")),
                        longVal(row.get("teacher_id")),
                        longVal(row.get("class_id")),
                        date(row.get("stat_day")),
                        longVal(row.get("checked_in_count")),
                        longVal(row.get("total_count"))
                ))
                .toList();
    }

    private Map<Long, TeacherStatisticsDashboardVO.TaskRow> buildTeacherTaskTable(
            List<TaskMetaRow> tasks,
            List<SubmissionRow> submissions,
            List<ReviewRow> reviews,
            List<CompletionRow> completions
    ) {
        Map<Long, List<ReviewRow>> reviewsByTask = reviews.stream().collect(Collectors.groupingBy(ReviewRow::taskId));
        Map<Long, Long> submissionCounts = submissions.stream().collect(Collectors.groupingBy(SubmissionRow::taskId, Collectors.counting()));
        Map<Long, Long> completionCounts = completions.stream().collect(Collectors.groupingBy(CompletionRow::taskId, Collectors.counting()));
        Map<Long, TeacherStatisticsDashboardVO.TaskRow> rows = new LinkedHashMap<>();
        for (TaskMetaRow task : tasks) {
            List<ReviewRow> taskReviews = reviewsByTask.getOrDefault(task.id(), List.of());
            rows.put(task.id(), new TeacherStatisticsDashboardVO.TaskRow(
                    task.id(),
                    task.title(),
                    submissionCounts.getOrDefault(task.id(), 0L),
                    (long) taskReviews.size(),
                    averageScore(taskReviews.stream().map(ReviewRow::score).toList()),
                    completionCounts.getOrDefault(task.id(), 0L)
            ));
        }
        return rows;
    }

    private List<TeacherStatisticsDashboardVO.DeviceRequestRow> buildTeacherDeviceTable(List<TaskMetaRow> tasks, List<DeviceRequestRow> requests) {
        Map<Long, List<DeviceRequestRow>> rowsByTask = requests.stream().collect(Collectors.groupingBy(DeviceRequestRow::taskId));
        List<TeacherStatisticsDashboardVO.DeviceRequestRow> rows = new ArrayList<>();
        for (TaskMetaRow task : tasks) {
            List<DeviceRequestRow> taskRows = rowsByTask.getOrDefault(task.id(), List.of());
            rows.add(new TeacherStatisticsDashboardVO.DeviceRequestRow(
                    task.id(),
                    task.title(),
                    taskRows.stream().filter(row -> "PENDING".equalsIgnoreCase(row.status())).count(),
                    taskRows.stream().filter(row -> "APPROVED".equalsIgnoreCase(row.status())).count(),
                    taskRows.stream().filter(row -> "BORROWED".equalsIgnoreCase(row.status())).count(),
                    taskRows.stream().filter(row -> "RETURNED".equalsIgnoreCase(row.status())).count()
            ));
        }
        return rows;
    }

    private Map<Long, AdminStatisticsDashboardVO.TeacherRow> buildAdminTeacherTable(
            List<TaskMetaRow> tasks,
            List<SubmissionRow> submissions,
            List<ReviewRow> reviews,
            List<SessionAggregateRow> sessionRows,
            Map<Long, String> teacherNames
    ) {
        Map<Long, Set<Long>> taskIdsByTeacher = tasks.stream().collect(Collectors.groupingBy(TaskMetaRow::publisherId, Collectors.mapping(TaskMetaRow::id, Collectors.toSet())));
        Map<Long, Long> submissionCounts = submissions.stream().collect(Collectors.groupingBy(SubmissionRow::publisherId, Collectors.counting()));
        Map<Long, Long> reviewCounts = reviews.stream().collect(Collectors.groupingBy(ReviewRow::publisherId, Collectors.counting()));
        Map<Long, List<SessionAggregateRow>> sessionsByTeacher = sessionRows.stream().collect(Collectors.groupingBy(SessionAggregateRow::teacherId));
        Set<Long> teacherIds = new TreeSet<>();
        teacherIds.addAll(taskIdsByTeacher.keySet());
        teacherIds.addAll(submissionCounts.keySet());
        teacherIds.addAll(reviewCounts.keySet());
        teacherIds.addAll(sessionsByTeacher.keySet());
        Map<Long, AdminStatisticsDashboardVO.TeacherRow> rows = new LinkedHashMap<>();
        for (Long teacherId : teacherIds) {
            List<SessionAggregateRow> teacherSessions = sessionsByTeacher.getOrDefault(teacherId, List.of());
            rows.put(teacherId, new AdminStatisticsDashboardVO.TeacherRow(
                    teacherId,
                    teacherNames.getOrDefault(teacherId, "教师#" + teacherId),
                    taskIdsByTeacher.getOrDefault(teacherId, Set.of()).size(),
                    submissionCounts.getOrDefault(teacherId, 0L),
                    reviewCounts.getOrDefault(teacherId, 0L),
                    teacherSessions.stream().map(SessionAggregateRow::sessionId).distinct().count(),
                    weightedRate(teacherSessions.stream().mapToLong(SessionAggregateRow::checkedInCount).sum(), teacherSessions.stream().mapToLong(SessionAggregateRow::totalCount).sum())
            ));
        }
        return rows;
    }

    private List<AdminStatisticsDashboardVO.ClassRow> buildAdminClassTable(
            List<SubmissionRow> submissions,
            List<SessionClassAggregateRow> attendanceRows,
            Map<Long, Long> studentCountByClass,
            Map<Long, String> classNames
    ) {
        Map<Long, Long> submissionCounts = submissions.stream()
                .filter(row -> row.classId() != null)
                .collect(Collectors.groupingBy(SubmissionRow::classId, Collectors.counting()));
        Map<Long, List<SessionClassAggregateRow>> sessionsByClass = attendanceRows.stream().collect(Collectors.groupingBy(SessionClassAggregateRow::classId));
        Set<Long> classIds = new TreeSet<>();
        classIds.addAll(studentCountByClass.keySet());
        classIds.addAll(submissionCounts.keySet());
        classIds.addAll(sessionsByClass.keySet());
        List<AdminStatisticsDashboardVO.ClassRow> rows = new ArrayList<>();
        for (Long classId : classIds) {
            List<SessionClassAggregateRow> classSessions = sessionsByClass.getOrDefault(classId, List.of());
            rows.add(new AdminStatisticsDashboardVO.ClassRow(
                    classId,
                    classNames.getOrDefault(classId, "班级#" + classId),
                    studentCountByClass.getOrDefault(classId, 0L),
                    submissionCounts.getOrDefault(classId, 0L),
                    classSessions.stream().map(SessionClassAggregateRow::sessionId).distinct().count(),
                    weightedRate(classSessions.stream().mapToLong(SessionClassAggregateRow::checkedInCount).sum(), classSessions.stream().mapToLong(SessionClassAggregateRow::totalCount).sum())
            ));
        }
        rows.sort(Comparator.comparing(AdminStatisticsDashboardVO.ClassRow::className, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private StatisticsChartVO buildTaskTrend(List<SubmissionRow> submissions, List<ReviewRow> reviews, ResolvedFilters filters) {
        Map<LocalDate, Long> submissionCounts = submissions.stream().collect(Collectors.groupingBy(SubmissionRow::day, Collectors.counting()));
        Map<LocalDate, Long> reviewCounts = reviews.stream().collect(Collectors.groupingBy(ReviewRow::day, Collectors.counting()));
        List<String> categories = new ArrayList<>();
        List<BigDecimal> submissionSeries = new ArrayList<>();
        List<BigDecimal> reviewSeries = new ArrayList<>();
        for (LocalDate cursor = filters.from(); !cursor.isAfter(filters.to()); cursor = cursor.plusDays(1)) {
            categories.add(cursor.toString());
            submissionSeries.add(BigDecimal.valueOf(submissionCounts.getOrDefault(cursor, 0L)));
            reviewSeries.add(BigDecimal.valueOf(reviewCounts.getOrDefault(cursor, 0L)));
        }
        return new StatisticsChartVO(
                categories,
                List.of(
                        new StatisticsChartVO.Series("提交数", "line", submissionSeries),
                        new StatisticsChartVO.Series("批阅数", "line", reviewSeries)
                )
        );
    }

    private StatisticsChartVO buildAttendanceTrend(List<SessionAggregateRow> sessions, ResolvedFilters filters) {
        Map<LocalDate, Long> sessionCounts = sessions.stream()
                .collect(Collectors.groupingBy(SessionAggregateRow::day, Collectors.mapping(SessionAggregateRow::sessionId, Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));
        Map<LocalDate, Long> checkedInCounts = sessions.stream().collect(Collectors.groupingBy(SessionAggregateRow::day, Collectors.summingLong(SessionAggregateRow::checkedInCount)));
        Map<LocalDate, Long> totalCounts = sessions.stream().collect(Collectors.groupingBy(SessionAggregateRow::day, Collectors.summingLong(SessionAggregateRow::totalCount)));
        List<String> categories = new ArrayList<>();
        List<BigDecimal> sessionSeries = new ArrayList<>();
        List<BigDecimal> rateSeries = new ArrayList<>();
        for (LocalDate cursor = filters.from(); !cursor.isAfter(filters.to()); cursor = cursor.plusDays(1)) {
            categories.add(cursor.toString());
            sessionSeries.add(BigDecimal.valueOf(sessionCounts.getOrDefault(cursor, 0L)));
            rateSeries.add(weightedRate(checkedInCounts.getOrDefault(cursor, 0L), totalCounts.getOrDefault(cursor, 0L)));
        }
        return new StatisticsChartVO(
                categories,
                List.of(
                        new StatisticsChartVO.Series("签到场次数", "bar", sessionSeries),
                        new StatisticsChartVO.Series("到课率", "line", rateSeries)
                )
        );
    }

    private long resolveTeacherCount(Long teacherId, Long classId, List<TaskMetaRow> tasks, List<CourseMetaRow> courses, List<SessionAggregateRow> sessions, Set<Long> knownTeacherIds) {
        if (teacherId != null) {
            return knownTeacherIds.contains(teacherId) ? 1L : 0L;
        }
        if (classId == null) {
            return knownTeacherIds.size();
        }
        Set<Long> ids = new TreeSet<>();
        tasks.forEach(row -> ids.add(row.publisherId()));
        courses.forEach(row -> ids.add(row.teacherId()));
        sessions.forEach(row -> ids.add(row.teacherId()));
        return ids.size();
    }

    private long resolveClassCount(Long teacherId, Long classId, List<SubmissionRow> submissions, List<SessionClassAggregateRow> attendanceRows, Set<Long> knownClassIds) {
        if (classId != null) {
            return knownClassIds.contains(classId) ? 1L : 0L;
        }
        if (teacherId == null) {
            return knownClassIds.size();
        }
        Set<Long> ids = new TreeSet<>();
        submissions.stream().map(SubmissionRow::classId).filter(Objects::nonNull).forEach(ids::add);
        attendanceRows.stream().map(SessionClassAggregateRow::classId).forEach(ids::add);
        return ids.size();
    }

    private long resolveStudentCount(Long teacherId, Long classId, ResolvedFilters filters, Map<Long, Long> studentCountByClass) {
        if (classId != null) {
            if (teacherId == null) {
                return studentCountByClass.values().stream().mapToLong(Long::longValue).sum();
            }
            return jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT scoped.student_id)
                    FROM (
                        SELECT rs.student_id
                        FROM report_submission rs
                        JOIN exp_task t ON t.id = rs.task_id
                        JOIN sys_user su ON su.id = rs.student_id
                        WHERE t.publisher_id = :teacherId
                          AND su.class_id = :classId
                          AND rs.submitted_at BETWEEN :fromTime AND :toTime

                        UNION

                        SELECT tc.student_id
                        FROM task_completion tc
                        JOIN exp_task t ON t.id = tc.task_id
                        JOIN sys_user su ON su.id = tc.student_id
                        WHERE t.publisher_id = :teacherId
                          AND su.class_id = :classId
                          AND tc.status = 'CONFIRMED'
                          AND tc.confirmed_at BETWEEN :fromTime AND :toTime

                        UNION

                        SELECT dr.student_id
                        FROM task_device_request dr
                        JOIN exp_task t ON t.id = dr.task_id
                        JOIN sys_user su ON su.id = dr.student_id
                        WHERE t.publisher_id = :teacherId
                          AND su.class_id = :classId
                          AND dr.created_at BETWEEN :fromTime AND :toTime

                        UNION

                        SELECT e.student_id
                        FROM experiment_course_enrollment e
                        JOIN experiment_course ec ON ec.id = e.course_id
                        JOIN sys_user su ON su.id = e.student_id
                        WHERE ec.teacher_id = :teacherId
                          AND ec.semester_id = :semesterId
                          AND e.status = 'ENROLLED'
                          AND su.class_id = :classId
                    ) scoped
                    """, new MapSqlParameterSource()
                    .addValue("teacherId", teacherId)
                    .addValue("classId", classId)
                    .addValue("semesterId", filters.semester().getId())
                    .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                    .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX))), Long.class);
        }
        if (teacherId == null) {
            return jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM sys_user su
                    JOIN sys_user_role ur ON ur.user_id = su.id
                    JOIN sys_role sr ON sr.id = ur.role_id
                    WHERE su.enabled = TRUE
                      AND sr.code = 'ROLE_STUDENT'
                    """, new MapSqlParameterSource(), Long.class);
        }
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT scoped.student_id)
                FROM (
                    SELECT rs.student_id
                    FROM report_submission rs
                    JOIN exp_task t ON t.id = rs.task_id
                    WHERE t.publisher_id = :teacherId
                      AND rs.submitted_at BETWEEN :fromTime AND :toTime

                    UNION

                    SELECT tc.student_id
                    FROM task_completion tc
                    JOIN exp_task t ON t.id = tc.task_id
                    WHERE t.publisher_id = :teacherId
                      AND tc.status = 'CONFIRMED'
                      AND tc.confirmed_at BETWEEN :fromTime AND :toTime

                    UNION

                    SELECT dr.student_id
                    FROM task_device_request dr
                    JOIN exp_task t ON t.id = dr.task_id
                    WHERE t.publisher_id = :teacherId
                      AND dr.created_at BETWEEN :fromTime AND :toTime

                    UNION

                    SELECT e.student_id
                    FROM experiment_course_enrollment e
                    JOIN experiment_course ec ON ec.id = e.course_id
                    WHERE ec.teacher_id = :teacherId
                      AND ec.semester_id = :semesterId
                      AND e.status = 'ENROLLED'
                ) scoped
                """, new MapSqlParameterSource()
                .addValue("teacherId", teacherId)
                .addValue("semesterId", filters.semester().getId())
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX))), Long.class);
    }

    private BigDecimal averageScore(Collection<BigDecimal> scores) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (BigDecimal score : scores) {
            if (score == null) continue;
            sum = sum.add(score);
            count++;
        }
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal weightedRate(long checkedIn, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(checkedIn).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private void recordTeacherExport(AuthenticatedUser actor, String exportType, Map<String, Object> conditions) {
        ExportRecordEntity entity = new ExportRecordEntity();
        entity.setOperatorId(actor.userId());
        entity.setExportType(exportType);
        entity.setConditionJson(toJson(conditions));
        entity.setCreatedAt(LocalDateTime.now());
        exportRecordMapper.insert(entity);
    }

    private void recordAdminStatisticsExport(AuthenticatedUser actor, String section, AdminStatisticsDashboardVO.Filters filters) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("section", section);
        detail.put("semesterId", filters.semesterId());
        detail.put("from", String.valueOf(filters.from()));
        detail.put("to", String.valueOf(filters.to()));
        if (filters.teacherId() != null) {
            detail.put("teacherId", filters.teacherId());
        }
        if (filters.classId() != null) {
            detail.put("classId", filters.classId());
        }
        adminAuditService.record(actor, AdminAuditActions.AUDIT_EXPORT, "statistics_report", null, detail);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String cell(Object value) {
        if (value == null) return "\"\"";
        return "\"" + String.valueOf(value).replace("\"", "\"\"") + "\"";
    }

    private long longVal(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private Long nullableLong(Object value) {
        if (value == null) return null;
        return longVal(value);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bigDecimal) return bigDecimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDate date(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }

    private LocalDateTime time(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        return LocalDateTime.parse(String.valueOf(value));
    }

    private record ResolvedFilters(
            SemesterEntity semester,
            LocalDate from,
            LocalDate to
    ) {
    }

    private record TaskMetaRow(
            Long id,
            String title,
            String status,
            Long publisherId,
            String publisherName,
            Long experimentCourseId,
            LocalDateTime createdAt
    ) {
    }

    private record SubmissionRow(
            Long taskId,
            Long publisherId,
            Long studentId,
            Long classId,
            LocalDate day
    ) {
    }

    private record ReviewRow(
            Long taskId,
            Long publisherId,
            Long studentId,
            Long classId,
            LocalDate day,
            BigDecimal score
    ) {
    }

    private record CompletionRow(
            Long taskId,
            Long publisherId,
            Long studentId,
            Long classId
    ) {
    }

    private record DeviceRequestRow(
            Long taskId,
            Long publisherId,
            Long studentId,
            Long classId,
            String status
    ) {
    }

    private record SessionAggregateRow(
            Long sessionId,
            Long teacherId,
            Long experimentCourseId,
            LocalDate day,
            long checkedInCount,
            long totalCount
    ) {
    }

    private record SessionClassAggregateRow(
            Long sessionId,
            Long teacherId,
            Long classId,
            LocalDate day,
            long checkedInCount,
            long totalCount
    ) {
    }

    private record CourseMetaRow(
            Long id,
            String title,
            Long teacherId,
            String teacherName,
            long slotCount
    ) {
    }
}
