package cn.edu.jnu.labflowreport.statistics.service;

import cn.edu.jnu.labflowreport.admin.service.AdminAuditActions;
import cn.edu.jnu.labflowreport.admin.service.AdminAuditService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.export.ExcelExportService;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.util.ClassDisplayUtils;
import cn.edu.jnu.labflowreport.persistence.entity.ExportRecordEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExportRecordMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
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
    private final SysUserMapper sysUserMapper;
    private final OrgClassMapper orgClassMapper;
    private final ExcelExportService excelExportService;

    public StatisticsService(
            NamedParameterJdbcTemplate jdbcTemplate,
            SemesterMapper semesterMapper,
            ExportRecordMapper exportRecordMapper,
            ObjectMapper objectMapper,
            AdminAuditService adminAuditService,
            SysUserMapper sysUserMapper,
            OrgClassMapper orgClassMapper,
            ExcelExportService excelExportService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.semesterMapper = semesterMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.objectMapper = objectMapper;
        this.adminAuditService = adminAuditService;
        this.sysUserMapper = sysUserMapper;
        this.orgClassMapper = orgClassMapper;
        this.excelExportService = excelExportService;
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

    public byte[] exportTeacherTaskStatsExcel(AuthenticatedUser teacher, Long semesterId, LocalDate from, LocalDate to) {
        TeacherStatisticsDashboardVO dashboard = getTeacherDashboard(teacher, semesterId, from, to);
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        List<TaskMetaRow> tasks = loadTasks(teacher.userId(), null, filters, false);
        List<Map<String, Object>> submissionDetails = loadTaskSubmissionDetails(teacher.userId(), null, filters);
        List<Map<String, Object>> reviewDetails = loadTaskReviewDetails(teacher.userId(), null, filters);
        List<Map<String, Object>> completionDetails = loadTaskCompletionDetails(teacher.userId(), null, filters);
        var unsubmittedRows = buildUnsubmittedRows(tasks, submissionDetails);
        recordTeacherExport(teacher, "TEACHER_TASK_STATS_EXCEL", Map.of(
                "semesterId", dashboard.filters().semesterId(),
                "from", String.valueOf(dashboard.filters().from()),
                "to", String.valueOf(dashboard.filters().to())
        ));
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildTeacherFilterRows(dashboard.filters(), teacher)),
                new ExcelExportService.SheetSpec(
                        "汇总",
                        List.of("任务ID", "任务标题", "提交数", "批阅数", "平均分", "已确认完成"),
                        dashboard.tables().taskTable().stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.submissionCount(), item.reviewedSubmissionCount(), item.avgScore(), item.confirmedCompletionCount()))
                                .toList()
                ),
                new ExcelExportService.SheetSpec("提交明细", List.of("任务ID", "任务标题", "学生ID", "学生姓名", "用户名", "班级", "版本号", "提交状态", "提交时间"),
                        toRows(submissionDetails, "task_id", "task_title", "student_id", "student_display_name", "student_username", "class_display_name", "version_no", "submit_status", "submitted_at")),
                new ExcelExportService.SheetSpec("未提交名单", List.of("任务ID", "任务标题", "学生ID", "学生姓名", "用户名", "班级", "任务可见"),
                        unsubmittedRows),
                new ExcelExportService.SheetSpec("批阅明细", List.of("任务ID", "任务标题", "学生ID", "学生姓名", "用户名", "班级", "版本号", "分数", "批语", "批阅时间"),
                        toRows(reviewDetails, "task_id", "task_title", "student_id", "student_display_name", "student_username", "class_display_name", "version_no", "score", "comment", "reviewed_at")),
                new ExcelExportService.SheetSpec("完成登记明细", List.of("任务ID", "任务标题", "学生ID", "学生姓名", "用户名", "班级", "完成状态", "完成来源", "申请时间", "确认时间", "确认教师"),
                        toRows(completionDetails, "task_id", "task_title", "student_id", "student_display_name", "student_username", "class_display_name", "status", "completion_source", "requested_at", "confirmed_at", "confirmed_by_display_name"))
        ));
    }

    public byte[] exportTeacherExperimentCourseStatsExcel(AuthenticatedUser teacher, Long semesterId, LocalDate from, LocalDate to) {
        TeacherStatisticsDashboardVO dashboard = getTeacherDashboard(teacher, semesterId, from, to);
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        List<Map<String, Object>> enrollmentDetails = loadCourseEnrollmentDetails(teacher.userId(), null, filters);
        List<Map<String, Object>> instanceDetails = loadCourseInstanceDetails(teacher.userId(), null, filters);
        List<Map<String, Object>> attendanceDetails = loadCourseAttendanceDetails(teacher.userId(), null, filters);
        recordTeacherExport(teacher, "TEACHER_EXPERIMENT_COURSE_STATS_EXCEL", Map.of(
                "semesterId", dashboard.filters().semesterId(),
                "from", String.valueOf(dashboard.filters().from()),
                "to", String.valueOf(dashboard.filters().to())
        ));
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildTeacherFilterRows(dashboard.filters(), teacher)),
                new ExcelExportService.SheetSpec(
                        "汇总",
                        List.of("课程ID", "课程名称", "场次数", "有效报名人数", "签到场次数"),
                        dashboard.tables().experimentCourseTable().stream()
                                .map(item -> row(item.courseId(), item.courseTitle(), item.slotCount(), item.activeEnrollmentCount(), item.attendanceSessionCount()))
                                .toList()
                ),
                new ExcelExportService.SheetSpec("课程报名明细",
                        List.of("课程ID", "课程名称", "场次ID", "场次名称", "学生ID", "学生姓名", "用户名", "班级", "报名方式", "报名时间", "状态"),
                        toRows(enrollmentDetails, "course_id", "course_title", "slot_id", "slot_name", "student_id", "student_display_name", "student_username", "class_display_name", "join_source", "selected_at", "status")),
                new ExcelExportService.SheetSpec("课程课次明细",
                        List.of("课程ID", "课程名称", "场次ID", "场次名称", "课次ID", "课次名称", "周几", "上课日期", "节次", "实验室", "容量"),
                        instanceRowsWithWeekday(instanceDetails)),
                new ExcelExportService.SheetSpec("课程签到明细",
                        List.of("课程ID", "课程名称", "场次ID", "场次名称", "课次ID", "课次名称", "周几", "上课日期", "节次", "实验室", "学生姓名", "用户名", "班级", "签到状态", "签到方式", "签到时间"),
                        attendanceRowsWithWeekday(attendanceDetails))
        ));
    }

    public byte[] exportTeacherDeviceRequestStatsExcel(AuthenticatedUser teacher, Long semesterId, LocalDate from, LocalDate to) {
        TeacherStatisticsDashboardVO dashboard = getTeacherDashboard(teacher, semesterId, from, to);
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        List<Map<String, Object>> requestDetails = loadDeviceRequestDetails(teacher.userId(), null, filters);
        recordTeacherExport(teacher, "TEACHER_DEVICE_REQUEST_STATS_EXCEL", Map.of(
                "semesterId", dashboard.filters().semesterId(),
                "from", String.valueOf(dashboard.filters().from()),
                "to", String.valueOf(dashboard.filters().to())
        ));
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildTeacherFilterRows(dashboard.filters(), teacher)),
                new ExcelExportService.SheetSpec(
                        "汇总",
                        List.of("任务ID", "任务标题", "待审批", "已批准", "借出中", "已归还"),
                        dashboard.tables().deviceRequestTable().stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.pendingCount(), item.approvedCount(), item.borrowedCount(), item.returnedCount()))
                                .toList()
                ),
                new ExcelExportService.SheetSpec("申请明细",
                        List.of("任务ID", "任务标题", "学生ID", "学生姓名", "用户名", "班级", "设备编码", "设备名称", "数量", "申请时间", "当前状态", "备注"),
                        toRows(requestDetails, "task_id", "task_title", "student_id", "student_display_name", "student_username", "class_display_name", "device_code", "device_name", "quantity", "created_at", "status", "note")),
                new ExcelExportService.SheetSpec("审批流转明细",
                        List.of("任务ID", "任务标题", "学生ID", "学生姓名", "用户名", "设备编码", "设备名称", "当前状态", "批准人", "批准时间", "驳回人", "驳回时间", "借出登记人", "借出时间", "归还登记人", "归还时间"),
                        toRows(requestDetails, "task_id", "task_title", "student_id", "student_display_name", "student_username", "device_code", "device_name", "status", "approved_by_name", "approved_at", "rejected_by_name", "rejected_at", "checkout_by_name", "checkout_at", "return_by_name", "return_at"))
        ));
    }

    public byte[] exportAdminTeacherStatsExcel(AuthenticatedUser admin, Long semesterId, LocalDate from, LocalDate to, Long teacherId, Long classId) {
        AdminStatisticsDashboardVO dashboard = getAdminDashboard(admin, semesterId, from, to, teacherId, classId);
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        List<Map<String, Object>> taskDetails = loadAdminTaskDetails(teacherId, classId, filters);
        List<Map<String, Object>> submissionDetails = loadTaskSubmissionDetails(teacherId, classId, filters);
        List<Map<String, Object>> attendanceDetails = loadAdminAttendanceDetails(teacherId, classId, filters);
        List<Map<String, Object>> courseDetails = loadAdminCourseDetails(teacherId, classId, filters);
        recordAdminStatisticsExport(admin, "teachers_excel", dashboard.filters());
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildAdminFilterRows(dashboard.filters(), admin)),
                new ExcelExportService.SheetSpec("汇总", List.of("教师ID", "教师姓名", "任务数", "提交数", "批阅数", "签到场次", "平均到课率"),
                        dashboard.tables().teacherTable().stream()
                                .map(item -> row(item.teacherId(), item.teacherName(), item.taskCount(), item.submissionCount(), item.reviewedSubmissionCount(), item.attendanceSessionCount(), item.avgAttendanceRate()))
                                .toList()),
                new ExcelExportService.SheetSpec("教师任务明细", List.of("教师ID", "教师姓名", "任务ID", "任务标题", "任务状态", "创建时间"),
                        toRows(taskDetails, "teacher_id", "teacher_name", "task_id", "task_title", "task_status", "created_at")),
                new ExcelExportService.SheetSpec("教师提交明细", List.of("教师ID", "教师姓名", "任务ID", "任务标题", "学生ID", "学生姓名", "用户名", "班级", "版本号", "提交时间"),
                        toRows(submissionDetails, "teacher_id", "teacher_name", "task_id", "task_title", "student_id", "student_display_name", "student_username", "class_display_name", "version_no", "submitted_at")),
                new ExcelExportService.SheetSpec("教师签到明细", List.of("教师ID", "教师姓名", "课程", "日期", "实验室", "签到场次", "已到人数", "应到人数", "到课率"),
                        toRows(attendanceDetails, "teacher_id", "teacher_name", "course_name", "lesson_date", "lab_room_name", "session_id", "checked_in_count", "total_count", "attendance_rate")),
                new ExcelExportService.SheetSpec("教师实验课程明细", List.of("教师ID", "教师姓名", "课程ID", "课程名称", "场次数", "有效报名数", "签到场次数"),
                        toRows(courseDetails, "teacher_id", "teacher_name", "course_id", "course_title", "slot_count", "active_enrollment_count", "attendance_session_count"))
        ));
    }

    public byte[] exportAdminClassStatsExcel(AuthenticatedUser admin, Long semesterId, LocalDate from, LocalDate to, Long teacherId, Long classId) {
        AdminStatisticsDashboardVO dashboard = getAdminDashboard(admin, semesterId, from, to, teacherId, classId);
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        List<Map<String, Object>> studentDetails = loadAdminStudentDetails(teacherId, classId, filters);
        List<Map<String, Object>> submissionDetails = loadTaskSubmissionDetails(teacherId, classId, filters);
        List<Map<String, Object>> attendanceDetails = loadAdminAttendanceDetails(teacherId, classId, filters);
        List<Map<String, Object>> enrollmentDetails = loadCourseEnrollmentDetails(teacherId, classId, filters);
        recordAdminStatisticsExport(admin, "classes_excel", dashboard.filters());
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildAdminFilterRows(dashboard.filters(), admin)),
                new ExcelExportService.SheetSpec("汇总", List.of("班级ID", "班级名称", "学生数", "提交数", "签到场次", "平均到课率"),
                        dashboard.tables().classTable().stream()
                                .map(item -> row(item.classId(), item.className(), item.studentCount(), item.submissionCount(), item.attendanceSessionCount(), item.avgAttendanceRate()))
                                .toList()),
                new ExcelExportService.SheetSpec("班级学生名单", List.of("班级ID", "班级名称", "学生ID", "学生姓名", "用户名"),
                        toRows(studentDetails, "class_id", "class_display_name", "student_id", "student_display_name", "student_username")),
                new ExcelExportService.SheetSpec("班级提交明细", List.of("班级ID", "班级名称", "任务ID", "任务标题", "学生ID", "学生姓名", "用户名", "版本号", "提交时间"),
                        toRows(submissionDetails, "class_id", "class_display_name", "task_id", "task_title", "student_id", "student_display_name", "student_username", "version_no", "submitted_at")),
                new ExcelExportService.SheetSpec("班级签到明细", List.of("班级ID", "班级名称", "课程", "日期", "实验室", "签到场次", "已到人数", "应到人数", "到课率"),
                        toRows(attendanceDetails, "class_id", "class_display_name", "course_name", "lesson_date", "lab_room_name", "session_id", "checked_in_count", "total_count", "attendance_rate")),
                new ExcelExportService.SheetSpec("班级实验课程报名明细", List.of("班级ID", "班级名称", "课程ID", "课程名称", "场次名称", "学生ID", "学生姓名", "用户名", "报名方式", "报名时间", "状态"),
                        toRows(enrollmentDetails, "class_id", "class_display_name", "course_id", "course_title", "slot_name", "student_id", "student_display_name", "student_username", "join_source", "selected_at", "status"))
        ));
    }

    public byte[] exportAdminExperimentCourseStatsExcel(AuthenticatedUser admin, Long semesterId, LocalDate from, LocalDate to, Long teacherId, Long classId) {
        AdminStatisticsDashboardVO dashboard = getAdminDashboard(admin, semesterId, from, to, teacherId, classId);
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        List<Map<String, Object>> instanceDetails = loadCourseInstanceDetails(teacherId, classId, filters);
        List<Map<String, Object>> enrollmentDetails = loadCourseEnrollmentDetails(teacherId, classId, filters);
        List<Map<String, Object>> attendanceDetails = loadCourseAttendanceDetails(teacherId, classId, filters);
        recordAdminStatisticsExport(admin, "experiment_courses_excel", dashboard.filters());
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildAdminFilterRows(dashboard.filters(), admin)),
                new ExcelExportService.SheetSpec("汇总", List.of("课程ID", "课程名称", "教师ID", "教师姓名", "有效报名数", "场次数", "签到场次数"),
                        dashboard.tables().experimentCourseTable().stream()
                                .map(item -> row(item.courseId(), item.courseTitle(), item.teacherId(), item.teacherName(), item.activeEnrollmentCount(), item.slotCount(), item.attendanceSessionCount()))
                                .toList()),
                new ExcelExportService.SheetSpec("课程课次明细", List.of("课程ID", "课程名称", "教师ID", "教师姓名", "场次ID", "场次名称", "课次ID", "课次名称", "周几", "上课日期", "节次", "实验室", "容量"),
                        adminInstanceRowsWithWeekday(instanceDetails)),
                new ExcelExportService.SheetSpec("课程报名明细", List.of("课程ID", "课程名称", "教师ID", "教师姓名", "场次ID", "场次名称", "学生ID", "学生姓名", "用户名", "班级", "报名方式", "报名时间", "状态"),
                        toRows(enrollmentDetails, "course_id", "course_title", "teacher_id", "teacher_name", "slot_id", "slot_name", "student_id", "student_display_name", "student_username", "class_display_name", "join_source", "selected_at", "status")),
                new ExcelExportService.SheetSpec("课程签到明细", List.of("课程ID", "课程名称", "教师ID", "教师姓名", "场次ID", "场次名称", "课次ID", "课次名称", "周几", "上课日期", "节次", "实验室", "学生姓名", "用户名", "班级", "签到状态", "签到方式", "签到时间"),
                        adminAttendanceRowsWithWeekday(attendanceDetails))
        ));
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

    private List<List<?>> buildTeacherFilterRows(TeacherStatisticsDashboardVO.Filters filters, AuthenticatedUser actor) {
        List<List<?>> rows = new ArrayList<>();
        rows.add(row("学期ID", filters.semesterId()));
        rows.add(row("学期名称", filters.semesterName()));
        rows.add(row("开始日期", filters.from()));
        rows.add(row("结束日期", filters.to()));
        rows.add(row("导出时间", LocalDateTime.now()));
        rows.add(row("操作者", actor.username()));
        return rows;
    }

    private List<List<?>> buildAdminFilterRows(AdminStatisticsDashboardVO.Filters filters, AuthenticatedUser actor) {
        List<List<?>> rows = new ArrayList<>();
        rows.add(row("学期ID", filters.semesterId()));
        rows.add(row("学期名称", filters.semesterName()));
        rows.add(row("开始日期", filters.from()));
        rows.add(row("结束日期", filters.to()));
        rows.add(row("教师ID", filters.teacherId()));
        rows.add(row("班级ID", filters.classId()));
        rows.add(row("导出时间", LocalDateTime.now()));
        rows.add(row("操作者", actor.username()));
        return rows;
    }

    private List<?> row(Object... values) {
        List<Object> row = new ArrayList<>();
        for (Object value : values) {
            row.add(value);
        }
        return row;
    }

    private List<List<?>> toRows(List<Map<String, Object>> maps, String... keys) {
        List<List<?>> rows = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            List<Object> row = new ArrayList<>();
            for (String key : keys) {
                row.add(map.get(key));
            }
            rows.add(row);
        }
        return rows;
    }

    private List<List<?>> instanceRowsWithWeekday(List<Map<String, Object>> maps) {
        List<List<?>> rows = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            rows.add(row(
                    map.get("course_id"),
                    map.get("course_title"),
                    map.get("slot_id"),
                    map.get("slot_name"),
                    map.get("instance_id"),
                    map.get("instance_name"),
                    weekdayLabel(date(map.get("lesson_date"))),
                    map.get("lesson_date"),
                    map.get("time_slot_name"),
                    map.get("lab_room_name"),
                    map.get("capacity")
            ));
        }
        return rows;
    }

    private List<List<?>> adminInstanceRowsWithWeekday(List<Map<String, Object>> maps) {
        List<List<?>> rows = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            rows.add(row(
                    map.get("course_id"),
                    map.get("course_title"),
                    map.get("teacher_id"),
                    map.get("teacher_name"),
                    map.get("slot_id"),
                    map.get("slot_name"),
                    map.get("instance_id"),
                    map.get("instance_name"),
                    weekdayLabel(date(map.get("lesson_date"))),
                    map.get("lesson_date"),
                    map.get("time_slot_name"),
                    map.get("lab_room_name"),
                    map.get("capacity")
            ));
        }
        return rows;
    }

    private List<List<?>> attendanceRowsWithWeekday(List<Map<String, Object>> maps) {
        List<List<?>> rows = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            rows.add(row(
                    map.get("course_id"),
                    map.get("course_title"),
                    map.get("slot_id"),
                    map.get("slot_name"),
                    map.get("instance_id"),
                    map.get("instance_name"),
                    weekdayLabel(date(map.get("lesson_date"))),
                    map.get("lesson_date"),
                    map.get("time_slot_name"),
                    map.get("lab_room_name"),
                    map.get("student_display_name"),
                    map.get("student_username"),
                    map.get("class_display_name"),
                    map.get("attendance_status"),
                    map.get("attendance_method"),
                    map.get("checked_in_at")
            ));
        }
        return rows;
    }

    private List<List<?>> adminAttendanceRowsWithWeekday(List<Map<String, Object>> maps) {
        List<List<?>> rows = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            rows.add(row(
                    map.get("course_id"),
                    map.get("course_title"),
                    map.get("teacher_id"),
                    map.get("teacher_name"),
                    map.get("slot_id"),
                    map.get("slot_name"),
                    map.get("instance_id"),
                    map.get("instance_name"),
                    weekdayLabel(date(map.get("lesson_date"))),
                    map.get("lesson_date"),
                    map.get("time_slot_name"),
                    map.get("lab_room_name"),
                    map.get("student_display_name"),
                    map.get("student_username"),
                    map.get("class_display_name"),
                    map.get("attendance_status"),
                    map.get("attendance_method"),
                    map.get("checked_in_at")
            ));
        }
        return rows;
    }

    private List<List<?>> buildUnsubmittedRows(List<TaskMetaRow> tasks, List<Map<String, Object>> submissionDetails) {
        Map<Long, Set<Long>> submittedStudentIds = submissionDetails.stream()
                .collect(Collectors.groupingBy(
                        map -> longVal(map.get("task_id")),
                        Collectors.mapping(map -> longVal(map.get("student_id")), Collectors.toSet())
                ));
        List<List<?>> rows = new ArrayList<>();
        for (TaskMetaRow task : tasks) {
            List<cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity> students = sysUserMapper.findStudentsForTask(task.id());
            Set<Long> submitted = submittedStudentIds.getOrDefault(task.id(), Set.of());
            Map<Long, String> classDisplayMap = loadClassDisplayMap(students);
            for (cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity student : students) {
                if (submitted.contains(student.getId())) {
                    continue;
                }
                rows.add(row(
                        task.id(),
                        task.title(),
                        student.getId(),
                        str(student.getDisplayName()),
                        student.getUsername(),
                        classDisplayMap.get(student.getId()),
                        "是"
                ));
            }
        }
        return rows;
    }

    private Map<Long, String> loadClassDisplayMap(List<cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity> students) {
        Set<Long> classIds = students.stream()
                .map(cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, OrgClassEntity> classMap = orgClassMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(OrgClassEntity::getId, item -> item));
        Map<Long, String> result = new LinkedHashMap<>();
        for (cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity student : students) {
            if (student.getId() == null) {
                continue;
            }
            OrgClassEntity clazz = classMap.get(student.getClassId());
            result.put(student.getId(),
                    clazz == null ? null : ClassDisplayUtils.effectiveDisplayName(clazz.getGrade(), clazz.getName()));
        }
        return result;
    }

    private String weekdayLabel(LocalDate lessonDate) {
        if (lessonDate == null) {
            return null;
        }
        return switch (lessonDate.getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private List<Map<String, Object>> loadTaskSubmissionDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.publisher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       t.id AS task_id,
                       t.title AS task_title,
                       su.id AS student_id,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS student_display_name,
                       su.username AS student_username,
                       su.class_id AS class_id,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       rs.version_no,
                       rs.submit_status,
                       rs.submitted_at
                FROM report_submission rs
                JOIN exp_task t ON t.id = rs.task_id
                JOIN sys_user teacher ON teacher.id = t.publisher_id
                JOIN sys_user su ON su.id = rs.student_id
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
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
        sql.append(" ORDER BY t.id DESC, su.username ASC, rs.version_no DESC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadTaskReviewDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.publisher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       t.id AS task_id,
                       t.title AS task_title,
                       su.id AS student_id,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS student_display_name,
                       su.username AS student_username,
                       su.class_id AS class_id,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       rs.version_no,
                       rr.score,
                       rr.comment,
                       rr.reviewed_at
                FROM report_review rr
                JOIN report_submission rs ON rs.id = rr.submission_id
                JOIN exp_task t ON t.id = rs.task_id
                JOIN sys_user teacher ON teacher.id = t.publisher_id
                JOIN sys_user su ON su.id = rs.student_id
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
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
        sql.append(" ORDER BY t.id DESC, su.username ASC, rr.reviewed_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadTaskCompletionDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.publisher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       t.id AS task_id,
                       t.title AS task_title,
                       su.id AS student_id,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS student_display_name,
                       su.username AS student_username,
                       su.class_id AS class_id,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       tc.status,
                       tc.completion_source,
                       tc.requested_at,
                       tc.confirmed_at,
                       COALESCE(NULLIF(TRIM(confirmer.display_name), ''), confirmer.username) AS confirmed_by_display_name
                FROM task_completion tc
                JOIN exp_task t ON t.id = tc.task_id
                JOIN sys_user teacher ON teacher.id = t.publisher_id
                JOIN sys_user su ON su.id = tc.student_id
                LEFT JOIN sys_user confirmer ON confirmer.id = tc.confirmed_by
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
                WHERE ((tc.requested_at BETWEEN :fromTime AND :toTime) OR (tc.confirmed_at BETWEEN :fromTime AND :toTime))
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
        sql.append(" ORDER BY t.id DESC, su.username ASC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadCourseEnrollmentDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT ec.id AS course_id,
                       ec.title AS course_title,
                       ec.teacher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       ecs.id AS slot_id,
                       COALESCE(NULLIF(TRIM(ecs.name), ''), CONCAT('场次', ecs.id)) AS slot_name,
                       su.class_id AS class_id,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       su.id AS student_id,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS student_display_name,
                       su.username AS student_username,
                       e.join_source,
                       e.selected_at,
                       e.status
                FROM experiment_course_enrollment e
                JOIN experiment_course ec ON ec.id = e.course_id
                JOIN sys_user teacher ON teacher.id = ec.teacher_id
                JOIN experiment_course_slot ecs ON ecs.id = e.slot_id
                JOIN sys_user su ON su.id = e.student_id
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
                WHERE ec.semester_id = :semesterId
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
        sql.append(" ORDER BY ec.id DESC, ecs.id ASC, e.selected_at ASC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadCourseInstanceDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT ec.id AS course_id,
                       ec.title AS course_title,
                       ec.teacher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       ecs.id AS slot_id,
                       COALESCE(NULLIF(TRIM(ecs.name), ''), CONCAT('场次', ecs.id)) AS slot_name,
                       ecsi.id AS instance_id,
                       ecsi.display_name AS instance_name,
                       ecsi.lesson_date,
                       ts.name AS time_slot_name,
                       lr.name AS lab_room_name,
                       ecsi.capacity
                FROM experiment_course_slot_instance ecsi
                JOIN experiment_course ec ON ec.id = ecsi.course_id
                JOIN sys_user teacher ON teacher.id = ec.teacher_id
                JOIN experiment_course_slot ecs ON ecs.id = ecsi.slot_group_id
                LEFT JOIN time_slot ts ON ts.id = ecsi.slot_id
                LEFT JOIN lab_room lr ON lr.id = ecsi.lab_room_id
                WHERE ec.semester_id = :semesterId
                  AND ecsi.lesson_date BETWEEN :fromDate AND :toDate
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("semesterId", filters.semester().getId())
                .addValue("fromDate", Date.valueOf(filters.from()))
                .addValue("toDate", Date.valueOf(filters.to()));
        if (teacherId != null) {
            sql.append(" AND ec.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append("""
                     AND EXISTS (
                        SELECT 1
                        FROM experiment_course_enrollment e
                        JOIN sys_user su ON su.id = e.student_id
                        WHERE e.slot_id = ecs.id
                          AND e.status = 'ENROLLED'
                          AND su.class_id = :classId
                     )
                    """);
            params.addValue("classId", classId);
        }
        sql.append(" ORDER BY ec.id DESC, ecsi.lesson_date ASC, ecsi.id ASC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadCourseAttendanceDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT ec.id AS course_id,
                       ec.title AS course_title,
                       ec.teacher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       ecs.id AS slot_id,
                       COALESCE(NULLIF(TRIM(ecs.name), ''), CONCAT('场次', ecs.id)) AS slot_name,
                       ecsi.id AS instance_id,
                       ecsi.display_name AS instance_name,
                       ecsi.lesson_date,
                       ts.name AS time_slot_name,
                       lr.name AS lab_room_name,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS student_display_name,
                       su.username AS student_username,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       COALESCE(ar.status, 'NOT_CHECKED_IN') AS attendance_status,
                       ar.method AS attendance_method,
                       ar.checked_in_at
                FROM attendance_session ats
                JOIN experiment_course ec ON ec.id = ats.experiment_course_id
                JOIN sys_user teacher ON teacher.id = ec.teacher_id
                JOIN experiment_course_slot ecs ON ecs.id = ats.experiment_course_slot_id
                JOIN experiment_course_slot_instance ecsi ON ecsi.id = ats.experiment_course_instance_id
                JOIN experiment_course_enrollment e ON e.slot_id = ats.experiment_course_slot_id AND e.status = 'ENROLLED'
                JOIN sys_user su ON su.id = e.student_id
                LEFT JOIN attendance_record ar ON ar.session_id = ats.id AND ar.student_id = su.id
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
                LEFT JOIN time_slot ts ON ts.id = ecsi.slot_id
                LEFT JOIN lab_room lr ON lr.id = ecsi.lab_room_id
                WHERE ats.source_type = 'EXPERIMENT_COURSE'
                  AND ec.semester_id = :semesterId
                  AND ats.started_at BETWEEN :fromTime AND :toTime
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("semesterId", filters.semester().getId())
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND ec.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        }
        sql.append(" ORDER BY ec.id DESC, ecsi.lesson_date ASC, su.username ASC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadDeviceRequestDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.publisher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       t.id AS task_id,
                       t.title AS task_title,
                       su.id AS student_id,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS student_display_name,
                       su.username AS student_username,
                       su.class_id AS class_id,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       td.code AS device_code,
                       td.name AS device_name,
                       dr.quantity,
                       dr.status,
                       dr.note,
                       dr.created_at,
                       COALESCE(NULLIF(TRIM(approver.display_name), ''), approver.username) AS approved_by_name,
                       dr.approved_at,
                       COALESCE(NULLIF(TRIM(rejector.display_name), ''), rejector.username) AS rejected_by_name,
                       dr.rejected_at,
                       COALESCE(NULLIF(TRIM(checkouter.display_name), ''), checkouter.username) AS checkout_by_name,
                       dr.checkout_at,
                       COALESCE(NULLIF(TRIM(returner.display_name), ''), returner.username) AS return_by_name,
                       dr.return_at
                FROM task_device_request dr
                JOIN exp_task t ON t.id = dr.task_id
                JOIN sys_user teacher ON teacher.id = t.publisher_id
                JOIN sys_user su ON su.id = dr.student_id
                JOIN thing_device td ON td.id = dr.device_id
                LEFT JOIN sys_user approver ON approver.id = dr.approved_by
                LEFT JOIN sys_user rejector ON rejector.id = dr.rejected_by
                LEFT JOIN sys_user checkouter ON checkouter.id = dr.checkout_by
                LEFT JOIN sys_user returner ON returner.id = dr.return_by
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
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
        sql.append(" ORDER BY t.id DESC, dr.created_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadAdminTaskDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.publisher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       t.id AS task_id,
                       t.title AS task_title,
                       t.status AS task_status,
                       t.created_at
                FROM exp_task t
                JOIN sys_user teacher ON teacher.id = t.publisher_id
                LEFT JOIN experiment_course ec ON ec.id = t.experiment_course_id
                WHERE ((t.experiment_course_id IS NOT NULL AND ec.semester_id = :semesterId)
                    OR (t.experiment_course_id IS NULL AND CAST(t.created_at AS DATE) BETWEEN :fromDate AND :toDate))
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("semesterId", filters.semester().getId())
                .addValue("fromDate", Date.valueOf(filters.from()))
                .addValue("toDate", Date.valueOf(filters.to()));
        if (teacherId != null) {
            sql.append(" AND t.publisher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append("""
                     AND (
                        EXISTS (SELECT 1 FROM exp_task_target_class tc WHERE tc.task_id = t.id AND tc.class_id = :classId)
                        OR EXISTS (
                           SELECT 1 FROM report_submission rs
                           JOIN sys_user su ON su.id = rs.student_id
                           WHERE rs.task_id = t.id AND su.class_id = :classId
                        )
                        OR EXISTS (
                           SELECT 1 FROM experiment_course_enrollment e
                           JOIN sys_user su ON su.id = e.student_id
                           WHERE e.course_id = t.experiment_course_id
                             AND e.status = 'ENROLLED'
                             AND su.class_id = :classId
                        )
                     )
                    """);
            params.addValue("classId", classId);
        }
        sql.append(" ORDER BY t.created_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadAdminAttendanceDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       su.class_id AS class_id,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN ec.title ELSE cs.course_name END AS course_name,
                       CASE WHEN s.source_type = 'EXPERIMENT_COURSE' THEN ecsi.lesson_date ELSE cs.lesson_date END AS lesson_date,
                       COALESCE(exp_room.name, class_room.name) AS lab_room_name,
                       s.id AS session_id,
                       COUNT(DISTINCT ar.student_id) AS checked_in_count,
                       COUNT(DISTINCT roster.student_id) AS total_count,
                       CASE
                         WHEN COUNT(DISTINCT roster.student_id) = 0 THEN 0
                         ELSE COUNT(DISTINCT ar.student_id) * 1.0 / COUNT(DISTINCT roster.student_id)
                       END AS attendance_rate
                FROM attendance_session s
                JOIN sys_user teacher ON teacher.id = s.teacher_id
                LEFT JOIN course_schedule cs ON cs.id = s.schedule_id AND s.source_type = 'CLASS_SCHEDULE'
                LEFT JOIN lab_room class_room ON class_room.id = cs.lab_room_id
                LEFT JOIN experiment_course ec ON ec.id = s.experiment_course_id AND s.source_type = 'EXPERIMENT_COURSE'
                LEFT JOIN experiment_course_slot_instance ecsi ON ecsi.id = s.experiment_course_instance_id AND s.source_type = 'EXPERIMENT_COURSE'
                LEFT JOIN lab_room exp_room ON exp_room.id = ecsi.lab_room_id
                LEFT JOIN (
                    SELECT ats.id AS session_id, su.id AS student_id, su.class_id
                    FROM attendance_session ats
                    JOIN sys_user su ON ats.source_type = 'CLASS_SCHEDULE' AND su.class_id = ats.class_id
                    JOIN sys_user_role ur ON ur.user_id = su.id
                    JOIN sys_role sr ON sr.id = ur.role_id AND sr.code = 'ROLE_STUDENT'
                    WHERE su.enabled = TRUE
                    UNION ALL
                    SELECT ats.id AS session_id, su.id AS student_id, su.class_id
                    FROM attendance_session ats
                    JOIN experiment_course_enrollment e ON ats.source_type = 'EXPERIMENT_COURSE'
                        AND e.slot_id = ats.experiment_course_slot_id AND e.status = 'ENROLLED'
                    JOIN sys_user su ON su.id = e.student_id
                ) roster ON roster.session_id = s.id
                LEFT JOIN sys_user su ON su.id = roster.student_id
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
                LEFT JOIN attendance_record ar ON ar.session_id = s.id AND ar.student_id = roster.student_id
                WHERE s.started_at BETWEEN :fromTime AND :toTime
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()))
                .addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        if (teacherId != null) {
            sql.append(" AND s.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        }
        sql.append(" GROUP BY s.teacher_id, teacher_name, su.class_id, class_display_name, course_name, lesson_date, lab_room_name, s.id ORDER BY lesson_date DESC, s.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadAdminCourseDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT ec.teacher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       ec.id AS course_id,
                       ec.title AS course_title,
                       COUNT(DISTINCT ecs.id) AS slot_count,
                       COUNT(DISTINCT CASE WHEN e.status = 'ENROLLED' THEN e.id END) AS active_enrollment_count,
                       COUNT(DISTINCT ats.id) AS attendance_session_count
                FROM experiment_course ec
                JOIN sys_user teacher ON teacher.id = ec.teacher_id
                LEFT JOIN experiment_course_slot ecs ON ecs.course_id = ec.id
                LEFT JOIN experiment_course_enrollment e ON e.course_id = ec.id
                LEFT JOIN sys_user su ON su.id = e.student_id
                LEFT JOIN attendance_session ats ON ats.experiment_course_id = ec.id
                WHERE ec.semester_id = :semesterId
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
        sql.append(" GROUP BY ec.teacher_id, teacher_name, ec.id, ec.title ORDER BY ec.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private List<Map<String, Object>> loadAdminStudentDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT su.class_id AS class_id,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       su.id AS student_id,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS student_display_name,
                       su.username AS student_username
                FROM sys_user su
                JOIN sys_user_role ur ON ur.user_id = su.id
                JOIN sys_role sr ON sr.id = ur.role_id
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
                WHERE su.enabled = TRUE
                  AND sr.code = 'ROLE_STUDENT'
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (classId != null) {
            sql.append(" AND su.class_id = :classId");
            params.addValue("classId", classId);
        } else if (teacherId != null) {
            sql.append("""
                     AND (
                         EXISTS (
                             SELECT 1
                             FROM report_submission rs
                             JOIN exp_task t ON t.id = rs.task_id
                             WHERE rs.student_id = su.id
                               AND t.publisher_id = :teacherId
                               AND rs.submitted_at BETWEEN :fromTime AND :toTime
                         )
                         OR EXISTS (
                             SELECT 1
                             FROM experiment_course_enrollment e
                             JOIN experiment_course ec ON ec.id = e.course_id
                             WHERE e.student_id = su.id
                               AND e.status = 'ENROLLED'
                               AND ec.teacher_id = :teacherId
                               AND ec.semester_id = :semesterId
                         )
                     )
                    """);
            params.addValue("teacherId", teacherId);
            params.addValue("semesterId", filters.semester().getId());
            params.addValue("fromTime", Timestamp.valueOf(filters.from().atStartOfDay()));
            params.addValue("toTime", Timestamp.valueOf(filters.to().atTime(LocalTime.MAX)));
        }
        sql.append(" ORDER BY class_display_name ASC, su.username ASC");
        return jdbcTemplate.queryForList(sql.toString(), params);
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

