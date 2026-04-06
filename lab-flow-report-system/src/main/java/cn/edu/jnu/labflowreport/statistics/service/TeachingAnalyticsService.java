package cn.edu.jnu.labflowreport.statistics.service;

import cn.edu.jnu.labflowreport.admin.service.AdminAuditActions;
import cn.edu.jnu.labflowreport.admin.service.AdminAuditService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.export.ExcelExportService;
import cn.edu.jnu.labflowreport.common.util.ClassDisplayUtils;
import cn.edu.jnu.labflowreport.config.PlagiarismProperties;
import cn.edu.jnu.labflowreport.persistence.entity.ExportRecordEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ExportRecordMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportReviewIssueTagMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.statistics.vo.StatisticsChartVO;
import cn.edu.jnu.labflowreport.statistics.vo.StatisticsOptionVO;
import cn.edu.jnu.labflowreport.statistics.vo.TeachingAnalyticsDashboardVO;
import cn.edu.jnu.labflowreport.workflow.ReviewIssueTags;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TeachingAnalyticsService {

    private static final String UNMARKED_TAG = "UNMARKED";
    private static final String UNMARKED_LABEL = "未标注";
    private static final BigDecimal ZERO_RATE = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal RISK_SCORE_THRESHOLD = BigDecimal.valueOf(60);
    private static final BigDecimal RISK_COMPLETION_THRESHOLD = BigDecimal.valueOf(0.60).setScale(4, RoundingMode.HALF_UP);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SemesterMapper semesterMapper;
    private final SysUserMapper sysUserMapper;
    private final OrgClassMapper orgClassMapper;
    private final ReportReviewIssueTagMapper reportReviewIssueTagMapper;
    private final ExcelExportService excelExportService;
    private final ExportRecordMapper exportRecordMapper;
    private final AdminAuditService adminAuditService;
    private final ObjectMapper objectMapper;
    private final PlagiarismProperties plagiarismProperties;

    public TeachingAnalyticsService(
            NamedParameterJdbcTemplate jdbcTemplate,
            SemesterMapper semesterMapper,
            SysUserMapper sysUserMapper,
            OrgClassMapper orgClassMapper,
            ReportReviewIssueTagMapper reportReviewIssueTagMapper,
            ExcelExportService excelExportService,
            ExportRecordMapper exportRecordMapper,
            AdminAuditService adminAuditService,
            ObjectMapper objectMapper,
            PlagiarismProperties plagiarismProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.semesterMapper = semesterMapper;
        this.sysUserMapper = sysUserMapper;
        this.orgClassMapper = orgClassMapper;
        this.reportReviewIssueTagMapper = reportReviewIssueTagMapper;
        this.excelExportService = excelExportService;
        this.exportRecordMapper = exportRecordMapper;
        this.adminAuditService = adminAuditService;
        this.objectMapper = objectMapper;
        this.plagiarismProperties = plagiarismProperties;
    }

    public TeachingAnalyticsDashboardVO getTeacherAnalytics(
            AuthenticatedUser teacher,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long classId,
            Long studentId
    ) {
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        return buildDashboard(filters, teacher.userId(), classId, studentId, false);
    }

    public TeachingAnalyticsDashboardVO getAdminAnalytics(
            AuthenticatedUser admin,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId,
            Long studentId
    ) {
        ResolvedFilters filters = resolveFilters(semesterId, from, to);
        return buildDashboard(filters, teacherId, classId, studentId, true);
    }

    public byte[] exportTeacherExperimentAnalyticsExcel(
            AuthenticatedUser actor,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long classId,
            Long studentId
    ) {
        TeachingAnalyticsDashboardVO dashboard = getTeacherAnalytics(actor, semesterId, from, to, classId, studentId);
        Map<String, Object> conditions = new LinkedHashMap<>();
        conditions.put("semesterId", dashboard.filters().semesterId());
        conditions.put("from", String.valueOf(dashboard.filters().from()));
        conditions.put("to", String.valueOf(dashboard.filters().to()));
        if (dashboard.filters().classId() != null) {
            conditions.put("classId", dashboard.filters().classId());
        }
        recordTeacherExport(actor, "TEACHER_TEACHING_EXPERIMENT_ANALYTICS", conditions);
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildFilterRows(actor, dashboard.filters())),
                new ExcelExportService.SheetSpec("实验完成率明细", List.of("任务ID", "任务标题", "教师", "可见学生数", "已提交人数", "已批阅人数", "已确认完成人数", "完成率", "平均分", "最高分", "最低分"),
                        dashboard.experimentAnalysis().taskTable().stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.teacherName(), item.visibleStudentCount(), item.submissionCount(), item.reviewedSubmissionCount(), item.confirmedCompletionCount(), item.completionRate(), item.avgScore(), item.maxScore(), item.minScore()))
                                .toList()),
                new ExcelExportService.SheetSpec("实验成绩明细", List.of("任务ID", "任务标题", "教师", "学生姓名", "用户名", "班级", "版本号", "分数", "批阅时间", "学期"),
                        loadReviewDetails(dashboard.filters().teacherId(), dashboard.filters().classId(), resolveFilters(dashboard.filters().semesterId(), dashboard.filters().from(), dashboard.filters().to())).stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.teacherName(), item.studentDisplayName(), item.studentUsername(), item.classDisplayName(), item.versionNo(), item.score(), item.reviewedAt(), dashboard.filters().semesterName()))
                                .toList())
        ));
    }

    public byte[] exportTeacherStudentAnalyticsExcel(
            AuthenticatedUser actor,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long classId,
            Long studentId
    ) {
        TeachingAnalyticsDashboardVO dashboard = getTeacherAnalytics(actor, semesterId, from, to, classId, studentId);
        Map<String, Object> conditions = new LinkedHashMap<>();
        conditions.put("semesterId", dashboard.filters().semesterId());
        conditions.put("from", String.valueOf(dashboard.filters().from()));
        conditions.put("to", String.valueOf(dashboard.filters().to()));
        if (dashboard.filters().classId() != null) {
            conditions.put("classId", dashboard.filters().classId());
        }
        if (dashboard.filters().studentId() != null) {
            conditions.put("studentId", dashboard.filters().studentId());
        }
        recordTeacherExport(actor, "TEACHER_TEACHING_STUDENT_ANALYTICS", conditions);
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildFilterRows(actor, dashboard.filters())),
                new ExcelExportService.SheetSpec("学生趋势明细", List.of("任务ID", "任务标题", "教师", "学生ID", "学生姓名", "用户名", "班级", "学生分数", "班级平均分", "学期"),
                        buildStudentTrendRows(dashboard, actor.userId(), dashboard.filters().classId(), dashboard.filters().studentId(), false)),
                new ExcelExportService.SheetSpec("风险学生名单", List.of("学生ID", "学生姓名", "用户名", "班级", "已提交任务数", "已批阅任务数", "平均分", "完成率", "最近3次平均分", "趋势", "风险原因"),
                        dashboard.studentAnalysis().riskStudentTable().stream()
                                .map(item -> row(item.studentId(), item.studentDisplayName(), item.studentUsername(), item.classDisplayName(), item.submittedTaskCount(), item.reviewedTaskCount(), item.avgScore(), item.completionRate(), item.recentThreeAvgScore(), item.trend(), item.riskReasons()))
                                .toList()),
                new ExcelExportService.SheetSpec("薄弱实验明细", List.of("任务ID", "任务标题", "教师", "平均分", "完成率", "未提交率"),
                        dashboard.studentAnalysis().weakTaskTable().stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.teacherName(), item.avgScore(), item.completionRate(), item.unsubmittedRate()))
                                .toList())
        ));
    }

    public byte[] exportTeacherReportQualityAnalyticsExcel(
            AuthenticatedUser actor,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long classId,
            Long studentId
    ) {
        TeachingAnalyticsDashboardVO dashboard = getTeacherAnalytics(actor, semesterId, from, to, classId, studentId);
        Map<String, Object> conditions = new LinkedHashMap<>();
        conditions.put("semesterId", dashboard.filters().semesterId());
        conditions.put("from", String.valueOf(dashboard.filters().from()));
        conditions.put("to", String.valueOf(dashboard.filters().to()));
        if (dashboard.filters().classId() != null) {
            conditions.put("classId", dashboard.filters().classId());
        }
        recordTeacherExport(actor, "TEACHER_TEACHING_REPORT_QUALITY_ANALYTICS", conditions);
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildFilterRows(actor, dashboard.filters())),
                new ExcelExportService.SheetSpec("错误标签统计明细", List.of("标签代码", "标签名称", "出现次数", "涉及学生数", "对应平均分"),
                        dashboard.reportQualityAnalysis().issueTagTable().stream()
                                .map(item -> row(item.tagCode(), item.tagLabel(), item.occurrenceCount(), item.studentCount(), item.avgScore()))
                                .toList()),
                new ExcelExportService.SheetSpec("查重风险明细", List.of("任务ID", "任务标题", "教师", "学生姓名", "用户名", "班级", "相似度", "风险等级", "学期"),
                        loadPlagiarismRiskRows(dashboard.filters().teacherId(), dashboard.filters().classId(), resolveFilters(dashboard.filters().semesterId(), dashboard.filters().from(), dashboard.filters().to())).stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.teacherName(), item.studentDisplayName(), item.studentUsername(), item.classDisplayName(), item.maxScore(), riskLabel(item.maxScore()), dashboard.filters().semesterName()))
                                .toList())
        ));
    }

    public byte[] exportAdminExperimentAnalyticsExcel(
            AuthenticatedUser actor,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId,
            Long studentId
    ) {
        TeachingAnalyticsDashboardVO dashboard = getAdminAnalytics(actor, semesterId, from, to, teacherId, classId, studentId);
        recordAdminExport(actor, "teaching_experiment_analytics", dashboard.filters());
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildFilterRows(actor, dashboard.filters())),
                new ExcelExportService.SheetSpec("实验完成率明细", List.of("任务ID", "任务标题", "教师", "可见学生数", "已提交人数", "已批阅人数", "已确认完成人数", "完成率", "平均分", "最高分", "最低分"),
                        dashboard.experimentAnalysis().taskTable().stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.teacherName(), item.visibleStudentCount(), item.submissionCount(), item.reviewedSubmissionCount(), item.confirmedCompletionCount(), item.completionRate(), item.avgScore(), item.maxScore(), item.minScore()))
                                .toList()),
                new ExcelExportService.SheetSpec("实验成绩明细", List.of("任务ID", "任务标题", "教师", "学生姓名", "用户名", "班级", "版本号", "分数", "批阅时间", "学期"),
                        loadReviewDetails(dashboard.filters().teacherId(), dashboard.filters().classId(), resolveFilters(dashboard.filters().semesterId(), dashboard.filters().from(), dashboard.filters().to())).stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.teacherName(), item.studentDisplayName(), item.studentUsername(), item.classDisplayName(), item.versionNo(), item.score(), item.reviewedAt(), dashboard.filters().semesterName()))
                                .toList())
        ));
    }

    public byte[] exportAdminStudentAnalyticsExcel(
            AuthenticatedUser actor,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId,
            Long studentId
    ) {
        TeachingAnalyticsDashboardVO dashboard = getAdminAnalytics(actor, semesterId, from, to, teacherId, classId, studentId);
        recordAdminExport(actor, "teaching_student_analytics", dashboard.filters());
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildFilterRows(actor, dashboard.filters())),
                new ExcelExportService.SheetSpec("学生趋势明细", List.of("任务ID", "任务标题", "教师", "学生ID", "学生姓名", "用户名", "班级", "学生分数", "班级平均分", "学期"),
                        buildStudentTrendRows(dashboard, dashboard.filters().teacherId(), dashboard.filters().classId(), dashboard.filters().studentId(), true)),
                new ExcelExportService.SheetSpec("风险学生名单", List.of("学生ID", "学生姓名", "用户名", "班级", "已提交任务数", "已批阅任务数", "平均分", "完成率", "最近3次平均分", "趋势", "风险原因"),
                        dashboard.studentAnalysis().riskStudentTable().stream()
                                .map(item -> row(item.studentId(), item.studentDisplayName(), item.studentUsername(), item.classDisplayName(), item.submittedTaskCount(), item.reviewedTaskCount(), item.avgScore(), item.completionRate(), item.recentThreeAvgScore(), item.trend(), item.riskReasons()))
                                .toList()),
                new ExcelExportService.SheetSpec("薄弱实验明细", List.of("任务ID", "任务标题", "教师", "平均分", "完成率", "未提交率"),
                        dashboard.studentAnalysis().weakTaskTable().stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.teacherName(), item.avgScore(), item.completionRate(), item.unsubmittedRate()))
                                .toList())
        ));
    }

    public byte[] exportAdminReportQualityAnalyticsExcel(
            AuthenticatedUser actor,
            Long semesterId,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId,
            Long studentId
    ) {
        TeachingAnalyticsDashboardVO dashboard = getAdminAnalytics(actor, semesterId, from, to, teacherId, classId, studentId);
        recordAdminExport(actor, "teaching_report_quality_analytics", dashboard.filters());
        return excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec("筛选条件", List.of("字段", "值"), buildFilterRows(actor, dashboard.filters())),
                new ExcelExportService.SheetSpec("错误标签统计明细", List.of("标签代码", "标签名称", "出现次数", "涉及学生数", "对应平均分"),
                        dashboard.reportQualityAnalysis().issueTagTable().stream()
                                .map(item -> row(item.tagCode(), item.tagLabel(), item.occurrenceCount(), item.studentCount(), item.avgScore()))
                                .toList()),
                new ExcelExportService.SheetSpec("查重风险明细", List.of("任务ID", "任务标题", "教师", "学生姓名", "用户名", "班级", "相似度", "风险等级", "学期"),
                        loadPlagiarismRiskRows(dashboard.filters().teacherId(), dashboard.filters().classId(), resolveFilters(dashboard.filters().semesterId(), dashboard.filters().from(), dashboard.filters().to())).stream()
                                .map(item -> row(item.taskId(), item.taskTitle(), item.teacherName(), item.studentDisplayName(), item.studentUsername(), item.classDisplayName(), item.maxScore(), riskLabel(item.maxScore()), dashboard.filters().semesterName()))
                                .toList())
        ));
    }

    private TeachingAnalyticsDashboardVO buildDashboard(ResolvedFilters filters, Long teacherId, Long classId, Long studentId, boolean adminView) {
        List<TaskMetaRow> tasks = loadTasks(teacherId, classId, filters, adminView);
        Map<Long, List<SysUserEntity>> visibleStudentsByTask = loadVisibleStudentsByTask(tasks, classId);
        Map<Long, String> classDisplayByStudentId = loadClassDisplayByStudentId(visibleStudentsByTask.values().stream().flatMap(Collection::stream).toList());
        List<StatisticsOptionVO> studentOptions = buildStudentOptions(visibleStudentsByTask, classDisplayByStudentId);
        Long resolvedStudentId = resolveStudentId(studentId, studentOptions);
        List<SubmissionDetailRow> submissionDetails = loadSubmissionDetails(teacherId, classId, filters);
        List<ReviewDetailRow> reviewDetails = loadReviewDetails(teacherId, classId, filters);
        List<CompletionDetailRow> completionDetails = loadCompletionDetails(teacherId, classId, filters);
        List<PlagiarismRiskRow> plagiarismRiskRows = loadPlagiarismRiskRows(teacherId, classId, filters);

        ExperimentComputation experimentComputation = buildExperimentAnalysis(tasks, visibleStudentsByTask, submissionDetails, reviewDetails, completionDetails);
        StudentComputation studentComputation = buildStudentAnalysis(tasks, visibleStudentsByTask, classDisplayByStudentId, submissionDetails, reviewDetails, completionDetails, resolvedStudentId);
        ReportQualityComputation qualityComputation = buildReportQualityAnalysis(reviewDetails, plagiarismRiskRows);

        return new TeachingAnalyticsDashboardVO(
                new TeachingAnalyticsDashboardVO.Filters(
                        filters.semester().getId(),
                        filters.semester().getName(),
                        filters.from(),
                        filters.to(),
                        teacherId,
                        classId,
                        resolvedStudentId,
                        loadSemesterOptions(),
                        adminView ? loadTeacherOptions() : List.of(),
                        buildClassOptions(visibleStudentsByTask, classDisplayByStudentId),
                        studentOptions
                ),
                experimentComputation.analysis(),
                studentComputation.analysis(),
                qualityComputation.analysis()
        );
    }

    private ExperimentComputation buildExperimentAnalysis(
            List<TaskMetaRow> tasks,
            Map<Long, List<SysUserEntity>> visibleStudentsByTask,
            List<SubmissionDetailRow> submissionDetails,
            List<ReviewDetailRow> reviewDetails,
            List<CompletionDetailRow> completionDetails
    ) {
        Map<TaskStudentKey, SubmissionDetailRow> latestSubmissionByKey = latestSubmissionsByKey(submissionDetails);
        Map<TaskStudentKey, ReviewDetailRow> latestReviewByKey = latestReviewsByKey(reviewDetails);
        Map<TaskStudentKey, CompletionDetailRow> confirmedCompletionByKey = confirmedCompletionsByKey(completionDetails);
        List<TeachingAnalyticsDashboardVO.ExperimentTaskRow> rows = new ArrayList<>();
        for (TaskMetaRow task : tasks) {
            Set<Long> visibleIds = visibleStudentsByTask.getOrDefault(task.id(), List.of()).stream()
                    .map(SysUserEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            long submissionCount = visibleIds.stream().filter(studentId -> latestSubmissionByKey.containsKey(new TaskStudentKey(task.id(), studentId))).count();
            List<ReviewDetailRow> latestReviews = visibleIds.stream()
                    .map(studentId -> latestReviewByKey.get(new TaskStudentKey(task.id(), studentId)))
                    .filter(Objects::nonNull)
                    .toList();
            long reviewedCount = latestReviews.size();
            long confirmedCount = visibleIds.stream().filter(studentId -> confirmedCompletionByKey.containsKey(new TaskStudentKey(task.id(), studentId))).count();
            List<BigDecimal> scores = latestReviews.stream().map(ReviewDetailRow::score).filter(Objects::nonNull).toList();
            rows.add(new TeachingAnalyticsDashboardVO.ExperimentTaskRow(
                    task.id(),
                    task.title(),
                    task.publisherName(),
                    visibleIds.size(),
                    submissionCount,
                    reviewedCount,
                    confirmedCount,
                    rate(confirmedCount, visibleIds.size()),
                    averageScore(scores),
                    maxScore(scores),
                    minScore(scores)
            ));
        }
        List<TeachingAnalyticsDashboardVO.ExperimentTaskRow> completionSorted = rows.stream()
                .sorted(Comparator.comparing(TeachingAnalyticsDashboardVO.ExperimentTaskRow::completionRate).reversed()
                        .thenComparing(TeachingAnalyticsDashboardVO.ExperimentTaskRow::taskTitle, Comparator.nullsLast(String::compareTo)))
                .toList();
        StatisticsChartVO completionChart = new StatisticsChartVO(
                completionSorted.stream().map(TeachingAnalyticsDashboardVO.ExperimentTaskRow::taskTitle).toList(),
                List.of(new StatisticsChartVO.Series("完成率", "bar", completionSorted.stream().map(TeachingAnalyticsDashboardVO.ExperimentTaskRow::completionRate).toList()))
        );
        List<TeachingAnalyticsDashboardVO.ExperimentTaskRow> scoreSorted = rows.stream()
                .sorted(Comparator.comparing(TeachingAnalyticsDashboardVO.ExperimentTaskRow::avgScore).reversed()
                        .thenComparing(TeachingAnalyticsDashboardVO.ExperimentTaskRow::taskTitle, Comparator.nullsLast(String::compareTo)))
                .toList();
        StatisticsChartVO scoreChart = new StatisticsChartVO(
                scoreSorted.stream().map(TeachingAnalyticsDashboardVO.ExperimentTaskRow::taskTitle).toList(),
                List.of(new StatisticsChartVO.Series("平均分", "bar", scoreSorted.stream().map(TeachingAnalyticsDashboardVO.ExperimentTaskRow::avgScore).toList()))
        );
        return new ExperimentComputation(new TeachingAnalyticsDashboardVO.ExperimentAnalysis(completionChart, scoreChart, rows));
    }

    private StudentComputation buildStudentAnalysis(
            List<TaskMetaRow> tasks,
            Map<Long, List<SysUserEntity>> visibleStudentsByTask,
            Map<Long, String> classDisplayByStudentId,
            List<SubmissionDetailRow> submissionDetails,
            List<ReviewDetailRow> reviewDetails,
            List<CompletionDetailRow> completionDetails,
            Long selectedStudentId
    ) {
        Map<TaskStudentKey, SubmissionDetailRow> latestSubmissionByKey = latestSubmissionsByKey(submissionDetails);
        Map<TaskStudentKey, ReviewDetailRow> latestReviewByKey = latestReviewsByKey(reviewDetails);
        Map<TaskStudentKey, CompletionDetailRow> confirmedCompletionByKey = confirmedCompletionsByKey(completionDetails);
        Map<Long, TaskMetaRow> taskMap = tasks.stream().collect(Collectors.toMap(TaskMetaRow::id, item -> item));
        Map<Long, Set<Long>> visibleTaskIdsByStudent = new LinkedHashMap<>();
        Map<Long, SysUserEntity> studentMap = new LinkedHashMap<>();
        for (Map.Entry<Long, List<SysUserEntity>> entry : visibleStudentsByTask.entrySet()) {
            for (SysUserEntity student : entry.getValue()) {
                if (student.getId() == null) continue;
                visibleTaskIdsByStudent.computeIfAbsent(student.getId(), key -> new LinkedHashSet<>()).add(entry.getKey());
                studentMap.putIfAbsent(student.getId(), student);
            }
        }

        List<TeachingAnalyticsDashboardVO.RiskStudentRow> riskRows = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> entry : visibleTaskIdsByStudent.entrySet()) {
            Long studentId = entry.getKey();
            List<TaskMetaRow> visibleTasks = entry.getValue().stream()
                    .map(taskMap::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(TaskMetaRow::createdAt, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(TaskMetaRow::id))
                    .toList();
            if (visibleTasks.isEmpty()) {
                continue;
            }
            List<BigDecimal> reviewedScores = new ArrayList<>();
            long submittedCount = 0;
            long reviewedCount = 0;
            long confirmedCount = 0;
            int consecutiveUnsubmitted = 0;
            int maxConsecutiveUnsubmitted = 0;
            int consecutiveLowScore = 0;
            int maxConsecutiveLowScore = 0;
            for (TaskMetaRow task : visibleTasks) {
                TaskStudentKey key = new TaskStudentKey(task.id(), studentId);
                boolean submitted = latestSubmissionByKey.containsKey(key);
                if (submitted) {
                    submittedCount++;
                    consecutiveUnsubmitted = 0;
                } else {
                    consecutiveUnsubmitted++;
                    maxConsecutiveUnsubmitted = Math.max(maxConsecutiveUnsubmitted, consecutiveUnsubmitted);
                }
                ReviewDetailRow review = latestReviewByKey.get(key);
                if (review != null && review.score() != null) {
                    reviewedCount++;
                    reviewedScores.add(review.score());
                    if (review.score().compareTo(RISK_SCORE_THRESHOLD) < 0) {
                        consecutiveLowScore++;
                        maxConsecutiveLowScore = Math.max(maxConsecutiveLowScore, consecutiveLowScore);
                    } else {
                        consecutiveLowScore = 0;
                    }
                } else {
                    consecutiveLowScore = 0;
                }
                if (confirmedCompletionByKey.containsKey(key)) {
                    confirmedCount++;
                }
            }
            BigDecimal avgScore = averageScore(reviewedScores);
            BigDecimal completionRate = rate(confirmedCount, visibleTasks.size());
            BigDecimal recentThreeAvg = averageScore(reviewedScores.stream().skip(Math.max(0, reviewedScores.size() - 3)).toList());
            List<String> reasons = new ArrayList<>();
            if (completionRate.compareTo(RISK_COMPLETION_THRESHOLD) < 0) reasons.add("完成率低于60%");
            if (!reviewedScores.isEmpty() && avgScore.compareTo(RISK_SCORE_THRESHOLD) < 0) reasons.add("平均分低于60");
            if (maxConsecutiveUnsubmitted >= 2) reasons.add("连续2次未提交");
            if (maxConsecutiveLowScore >= 2) reasons.add("连续2次低分");
            if (!reasons.isEmpty()) {
                SysUserEntity student = studentMap.get(studentId);
                riskRows.add(new TeachingAnalyticsDashboardVO.RiskStudentRow(
                        studentId,
                        student == null ? null : student.getDisplayName(),
                        student == null ? null : student.getUsername(),
                        classDisplayByStudentId.get(studentId),
                        submittedCount,
                        reviewedCount,
                        avgScore,
                        completionRate,
                        recentThreeAvg,
                        trendLabel(reviewedScores),
                        String.join("；", reasons)
                ));
            }
        }
        List<TeachingAnalyticsDashboardVO.RiskStudentRow> sortedRiskRows = riskRows.stream()
                .sorted(Comparator.comparing(TeachingAnalyticsDashboardVO.RiskStudentRow::completionRate)
                        .thenComparing(TeachingAnalyticsDashboardVO.RiskStudentRow::avgScore)
                        .thenComparing(TeachingAnalyticsDashboardVO.RiskStudentRow::studentUsername, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<TeachingAnalyticsDashboardVO.WeakTaskRow> weakRows = buildWeakTaskRows(tasks, visibleStudentsByTask, latestSubmissionByKey, latestReviewByKey, confirmedCompletionByKey);
        StatisticsChartVO trendChart = buildStudentTrendChart(tasks, visibleStudentsByTask, latestReviewByKey, studentMap, selectedStudentId);
        return new StudentComputation(new TeachingAnalyticsDashboardVO.StudentAnalysis(trendChart, sortedRiskRows, weakRows));
    }

    private List<TeachingAnalyticsDashboardVO.WeakTaskRow> buildWeakTaskRows(
            List<TaskMetaRow> tasks,
            Map<Long, List<SysUserEntity>> visibleStudentsByTask,
            Map<TaskStudentKey, SubmissionDetailRow> latestSubmissionByKey,
            Map<TaskStudentKey, ReviewDetailRow> latestReviewByKey,
            Map<TaskStudentKey, CompletionDetailRow> confirmedCompletionByKey
    ) {
        List<TeachingAnalyticsDashboardVO.WeakTaskRow> rows = new ArrayList<>();
        for (TaskMetaRow task : tasks) {
            Set<Long> visibleIds = visibleStudentsByTask.getOrDefault(task.id(), List.of()).stream()
                    .map(SysUserEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            long submittedCount = visibleIds.stream().filter(studentId -> latestSubmissionByKey.containsKey(new TaskStudentKey(task.id(), studentId))).count();
            long confirmedCount = visibleIds.stream().filter(studentId -> confirmedCompletionByKey.containsKey(new TaskStudentKey(task.id(), studentId))).count();
            List<BigDecimal> scores = visibleIds.stream()
                    .map(studentId -> latestReviewByKey.get(new TaskStudentKey(task.id(), studentId)))
                    .filter(Objects::nonNull)
                    .map(ReviewDetailRow::score)
                    .filter(Objects::nonNull)
                    .toList();
            rows.add(new TeachingAnalyticsDashboardVO.WeakTaskRow(
                    task.id(),
                    task.title(),
                    task.publisherName(),
                    averageScore(scores),
                    rate(confirmedCount, visibleIds.size()),
                    visibleIds.isEmpty() ? ZERO_RATE : rate(visibleIds.size() - submittedCount, visibleIds.size())
            ));
        }
        return rows.stream()
                .sorted(Comparator.comparing(TeachingAnalyticsDashboardVO.WeakTaskRow::avgScore)
                        .thenComparing(TeachingAnalyticsDashboardVO.WeakTaskRow::completionRate)
                        .thenComparing(TeachingAnalyticsDashboardVO.WeakTaskRow::unsubmittedRate, Comparator.reverseOrder()))
                .toList();
    }

    private StatisticsChartVO buildStudentTrendChart(
            List<TaskMetaRow> tasks,
            Map<Long, List<SysUserEntity>> visibleStudentsByTask,
            Map<TaskStudentKey, ReviewDetailRow> latestReviewByKey,
            Map<Long, SysUserEntity> studentMap,
            Long selectedStudentId
    ) {
        if (selectedStudentId == null || !studentMap.containsKey(selectedStudentId)) {
            return new StatisticsChartVO(List.of(), List.of(
                    new StatisticsChartVO.Series("学生分数", "line", List.of()),
                    new StatisticsChartVO.Series("班级平均分", "line", List.of())
            ));
        }
        SysUserEntity student = studentMap.get(selectedStudentId);
        Long classId = student == null ? null : student.getClassId();
        List<String> categories = new ArrayList<>();
        List<BigDecimal> studentSeries = new ArrayList<>();
        List<BigDecimal> classSeries = new ArrayList<>();
        for (TaskMetaRow task : tasks.stream().sorted(Comparator.comparing(TaskMetaRow::createdAt, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(TaskMetaRow::id)).toList()) {
            Set<Long> visibleIds = visibleStudentsByTask.getOrDefault(task.id(), List.of()).stream()
                    .map(SysUserEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!visibleIds.contains(selectedStudentId)) {
                continue;
            }
            categories.add(task.title());
            ReviewDetailRow review = latestReviewByKey.get(new TaskStudentKey(task.id(), selectedStudentId));
            studentSeries.add(review == null ? null : review.score());
            List<BigDecimal> classScores = visibleStudentsByTask.getOrDefault(task.id(), List.of()).stream()
                    .filter(item -> classId == null || Objects.equals(item.getClassId(), classId))
                    .map(item -> latestReviewByKey.get(new TaskStudentKey(task.id(), item.getId())))
                    .filter(Objects::nonNull)
                    .map(ReviewDetailRow::score)
                    .filter(Objects::nonNull)
                    .toList();
            classSeries.add(classScores.isEmpty() ? null : averageScore(classScores));
        }
        return new StatisticsChartVO(categories, List.of(
                new StatisticsChartVO.Series("学生分数", "line", studentSeries),
                new StatisticsChartVO.Series("班级平均分", "line", classSeries)
        ));
    }

    private ReportQualityComputation buildReportQualityAnalysis(List<ReviewDetailRow> reviewDetails, List<PlagiarismRiskRow> plagiarismRiskRows) {
        Map<String, TagAccumulator> accumulators = new LinkedHashMap<>();
        for (ReviewIssueTags.TagOption item : ReviewIssueTags.options()) {
            accumulators.put(item.code(), new TagAccumulator(item.label()));
        }
        accumulators.put(UNMARKED_TAG, new TagAccumulator(UNMARKED_LABEL));
        for (ReviewDetailRow review : reviewDetails) {
            List<String> tags = review.issueTags().isEmpty() ? List.of(UNMARKED_TAG) : review.issueTags();
            for (String tagCode : tags) {
                TagAccumulator accumulator = accumulators.computeIfAbsent(tagCode, key -> new TagAccumulator(ReviewIssueTags.labelOf(key)));
                accumulator.occurrenceCount++;
                if (review.studentId() != null) accumulator.studentIds.add(review.studentId());
                if (review.score() != null) accumulator.scores.add(review.score());
            }
        }
        List<TeachingAnalyticsDashboardVO.IssueTagRow> issueRows = accumulators.entrySet().stream()
                .filter(item -> item.getValue().occurrenceCount > 0)
                .map(item -> new TeachingAnalyticsDashboardVO.IssueTagRow(
                        item.getKey(),
                        item.getValue().label,
                        item.getValue().occurrenceCount,
                        item.getValue().studentIds.size(),
                        averageScore(item.getValue().scores)
                ))
                .sorted(Comparator.comparing(TeachingAnalyticsDashboardVO.IssueTagRow::occurrenceCount).reversed()
                        .thenComparing(TeachingAnalyticsDashboardVO.IssueTagRow::tagLabel))
                .toList();
        StatisticsChartVO issueChart = new StatisticsChartVO(
                issueRows.stream().map(TeachingAnalyticsDashboardVO.IssueTagRow::tagLabel).toList(),
                List.of(new StatisticsChartVO.Series("出现次数", "bar", issueRows.stream().map(item -> BigDecimal.valueOf(item.occurrenceCount())).toList()))
        );

        Map<String, Long> riskCounts = new LinkedHashMap<>();
        riskCounts.put("低风险", 0L);
        riskCounts.put("中风险", 0L);
        riskCounts.put("高风险", 0L);
        for (PlagiarismRiskRow row : plagiarismRiskRows) {
            String label = riskLabel(row.maxScore());
            riskCounts.put(label, riskCounts.getOrDefault(label, 0L) + 1);
        }
        StatisticsChartVO riskChart = new StatisticsChartVO(
                List.of("低风险", "中风险", "高风险"),
                List.of(new StatisticsChartVO.Series("报告数", "bar", List.of(
                        BigDecimal.valueOf(riskCounts.get("低风险")),
                        BigDecimal.valueOf(riskCounts.get("中风险")),
                        BigDecimal.valueOf(riskCounts.get("高风险"))
                )))
        );
        return new ReportQualityComputation(new TeachingAnalyticsDashboardVO.ReportQualityAnalysis(issueChart, riskChart, issueRows));
    }

    private List<List<?>> buildStudentTrendRows(TeachingAnalyticsDashboardVO dashboard, Long teacherId, Long classId, Long studentId, boolean adminView) {
        ResolvedFilters filters = resolveFilters(dashboard.filters().semesterId(), dashboard.filters().from(), dashboard.filters().to());
        List<TaskMetaRow> tasks = loadTasks(teacherId, classId, filters, adminView);
        Map<Long, List<SysUserEntity>> visibleStudentsByTask = loadVisibleStudentsByTask(tasks, classId);
        Map<TaskStudentKey, ReviewDetailRow> latestReviewByKey = latestReviewsByKey(loadReviewDetails(teacherId, classId, filters));
        Map<Long, SysUserEntity> studentMap = visibleStudentsByTask.values().stream()
                .flatMap(Collection::stream)
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(SysUserEntity::getId, item -> item, (a, b) -> a));
        if (studentId == null || !studentMap.containsKey(studentId)) {
            return List.of();
        }
        SysUserEntity selectedStudent = studentMap.get(studentId);
        Long selectedClassId = selectedStudent.getClassId();
        Map<Long, String> classDisplayByStudent = loadClassDisplayByStudentId(studentMap.values().stream().toList());
        List<List<?>> rows = new ArrayList<>();
        for (TaskMetaRow task : tasks.stream().sorted(Comparator.comparing(TaskMetaRow::createdAt, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(TaskMetaRow::id)).toList()) {
            Set<Long> visibleIds = visibleStudentsByTask.getOrDefault(task.id(), List.of()).stream()
                    .map(SysUserEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!visibleIds.contains(studentId)) {
                continue;
            }
            ReviewDetailRow studentReview = latestReviewByKey.get(new TaskStudentKey(task.id(), studentId));
            List<BigDecimal> classScores = visibleStudentsByTask.getOrDefault(task.id(), List.of()).stream()
                    .filter(item -> selectedClassId == null || Objects.equals(item.getClassId(), selectedClassId))
                    .map(item -> latestReviewByKey.get(new TaskStudentKey(task.id(), item.getId())))
                    .filter(Objects::nonNull)
                    .map(ReviewDetailRow::score)
                    .filter(Objects::nonNull)
                    .toList();
            rows.add(row(
                    task.id(),
                    task.title(),
                    task.publisherName(),
                    studentId,
                    selectedStudent.getDisplayName(),
                    selectedStudent.getUsername(),
                    classDisplayByStudent.get(studentId),
                    studentReview == null ? null : studentReview.score(),
                    classScores.isEmpty() ? null : averageScore(classScores),
                    dashboard.filters().semesterName()
            ));
        }
        return rows;
    }

    private List<ReviewDetailRow> loadReviewDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT rr.id AS review_id,
                       t.id AS task_id,
                       t.title AS task_title,
                       t.publisher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
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
        sql.append(" ORDER BY rr.reviewed_at DESC, rr.id DESC");
        List<ReviewDetailRow> rows = jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new ReviewDetailRow(
                        longVal(row.get("review_id")),
                        longVal(row.get("task_id")),
                        str(row.get("task_title")),
                        longVal(row.get("teacher_id")),
                        str(row.get("teacher_name")),
                        longVal(row.get("student_id")),
                        str(row.get("student_display_name")),
                        str(row.get("student_username")),
                        nullableLong(row.get("class_id")),
                        str(row.get("class_display_name")),
                        nullableLong(row.get("version_no")) == null ? null : nullableLong(row.get("version_no")).intValue(),
                        decimalOrNull(row.get("score")),
                        str(row.get("comment")),
                        time(row.get("reviewed_at")),
                        List.of()
                ))
                .toList();
        Map<Long, List<String>> tagMap = loadReviewIssueTags(rows.stream().map(ReviewDetailRow::reviewId).toList());
        return rows.stream()
                .map(item -> item.withIssueTags(tagMap.getOrDefault(item.reviewId(), List.of())))
                .toList();
    }

    private List<SubmissionDetailRow> loadSubmissionDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.id AS task_id,
                       t.title AS task_title,
                       t.publisher_id AS teacher_id,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
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
        sql.append(" ORDER BY rs.submitted_at DESC, rs.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new SubmissionDetailRow(
                        longVal(row.get("task_id")),
                        str(row.get("task_title")),
                        longVal(row.get("teacher_id")),
                        str(row.get("teacher_name")),
                        longVal(row.get("student_id")),
                        str(row.get("student_display_name")),
                        str(row.get("student_username")),
                        nullableLong(row.get("class_id")),
                        str(row.get("class_display_name")),
                        nullableLong(row.get("version_no")) == null ? null : nullableLong(row.get("version_no")).intValue(),
                        str(row.get("submit_status")),
                        time(row.get("submitted_at"))
                ))
                .toList();
    }

    private List<CompletionDetailRow> loadCompletionDetails(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.id AS task_id,
                       su.id AS student_id,
                       tc.status,
                       tc.confirmed_at
                FROM task_completion tc
                JOIN exp_task t ON t.id = tc.task_id
                JOIN sys_user su ON su.id = tc.student_id
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
        return jdbcTemplate.queryForList(sql.toString(), params).stream()
                .map(row -> new CompletionDetailRow(
                        longVal(row.get("task_id")),
                        longVal(row.get("student_id")),
                        str(row.get("status")),
                        time(row.get("confirmed_at"))
                ))
                .toList();
    }

    private List<PlagiarismRiskRow> loadPlagiarismRiskRows(Long teacherId, Long classId, ResolvedFilters filters) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.id AS task_id,
                       t.title AS task_title,
                       COALESCE(NULLIF(TRIM(teacher.display_name), ''), teacher.username) AS teacher_name,
                       su.id AS student_id,
                       COALESCE(NULLIF(TRIM(su.display_name), ''), su.username) AS student_display_name,
                       su.username AS student_username,
                       CASE
                         WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                         WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                         ELSE c.name
                       END AS class_display_name,
                       b.max_score
                FROM plag_submission_best_match b
                JOIN report_submission rs ON rs.id = b.submission_id
                JOIN exp_task t ON t.id = rs.task_id
                JOIN sys_user teacher ON teacher.id = t.publisher_id
                JOIN sys_user su ON su.id = rs.student_id
                LEFT JOIN org_class c ON c.id = su.class_id
                LEFT JOIN org_department d ON d.id = c.department_id
                JOIN (
                    SELECT task_id, MAX(id) AS latest_run_id
                    FROM plag_task_run
                    GROUP BY task_id
                ) latest ON latest.task_id = b.task_id AND latest.latest_run_id = b.run_id
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
                .map(row -> new PlagiarismRiskRow(
                        longVal(row.get("task_id")),
                        str(row.get("task_title")),
                        str(row.get("teacher_name")),
                        longVal(row.get("student_id")),
                        str(row.get("student_display_name")),
                        str(row.get("student_username")),
                        str(row.get("class_display_name")),
                        decimalOrNull(row.get("max_score"))
                ))
                .toList();
    }

    private Map<Long, List<String>> loadReviewIssueTags(Collection<Long> reviewIds) {
        List<Long> ids = reviewIds == null ? List.of() : reviewIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (ReportReviewIssueTagMapper.IssueTagRow row : reportReviewIssueTagMapper.findRowsByReviewIds(ids)) {
            result.computeIfAbsent(row.reviewId(), key -> new ArrayList<>()).add(row.tagCode());
        }
        return result;
    }

    private Map<Long, List<SysUserEntity>> loadVisibleStudentsByTask(List<TaskMetaRow> tasks, Long classId) {
        Map<Long, List<SysUserEntity>> result = new LinkedHashMap<>();
        for (TaskMetaRow task : tasks) {
            List<SysUserEntity> students = sysUserMapper.findStudentsForTask(task.id()).stream()
                    .filter(item -> item.getId() != null)
                    .filter(item -> classId == null || Objects.equals(item.getClassId(), classId))
                    .collect(Collectors.toMap(SysUserEntity::getId, item -> item, (a, b) -> a, LinkedHashMap::new))
                    .values()
                    .stream()
                    .toList();
            result.put(task.id(), students);
        }
        return result;
    }

    private Map<Long, String> loadClassDisplayByStudentId(Collection<SysUserEntity> students) {
        List<SysUserEntity> studentList = students == null ? List.of() : students.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getId() != null)
                .toList();
        if (studentList.isEmpty()) {
            return Map.of();
        }
        List<Long> classIds = studentList.stream()
                .map(SysUserEntity::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> classDisplayByClassId = new LinkedHashMap<>();
        if (!classIds.isEmpty()) {
            MapSqlParameterSource params = new MapSqlParameterSource().addValue("classIds", classIds);
            jdbcTemplate.queryForList("""
                    SELECT c.id AS id,
                           CASE
                             WHEN d.name IS NOT NULL AND c.grade IS NOT NULL THEN CONCAT(d.name, ' / ', c.grade, '级', c.name)
                             WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name)
                             ELSE c.name
                           END AS label
                    FROM org_class c
                    LEFT JOIN org_department d ON d.id = c.department_id
                    WHERE c.id IN (:classIds)
                    """, params).forEach(row -> classDisplayByClassId.put(longVal(row.get("id")), str(row.get("label"))));
        }
        Map<Long, String> result = new LinkedHashMap<>();
        for (SysUserEntity student : studentList) {
            result.put(student.getId(), classDisplayByClassId.get(student.getClassId()));
        }
        return result;
    }

    private List<StatisticsOptionVO> buildClassOptions(
            Map<Long, List<SysUserEntity>> visibleStudentsByTask,
            Map<Long, String> classDisplayByStudentId
    ) {
        Map<Long, String> classMap = new LinkedHashMap<>();
        visibleStudentsByTask.values().stream()
                .flatMap(Collection::stream)
                .forEach(student -> {
                    if (student.getClassId() != null) {
                        String label = classDisplayByStudentId.get(student.getId());
                        if (label != null && !label.isBlank()) {
                            classMap.put(student.getClassId(), label);
                        }
                    }
                });
        return classMap.entrySet().stream()
                .map(item -> new StatisticsOptionVO(item.getKey(), item.getValue()))
                .sorted(Comparator.comparing(StatisticsOptionVO::label, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<StatisticsOptionVO> buildStudentOptions(
            Map<Long, List<SysUserEntity>> visibleStudentsByTask,
            Map<Long, String> classDisplayByStudentId
    ) {
        Map<Long, SysUserEntity> studentMap = new LinkedHashMap<>();
        visibleStudentsByTask.values().stream().flatMap(Collection::stream).forEach(student -> {
            if (student.getId() != null) {
                studentMap.putIfAbsent(student.getId(), student);
            }
        });
        return studentMap.values().stream()
                .map(student -> new StatisticsOptionVO(student.getId(), buildStudentLabel(student, classDisplayByStudentId.get(student.getId()))))
                .sorted(Comparator.comparing(StatisticsOptionVO::label, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private String buildStudentLabel(SysUserEntity student, String classDisplayName) {
        String displayName = (student.getDisplayName() == null || student.getDisplayName().isBlank()) ? student.getUsername() : student.getDisplayName();
        if (classDisplayName == null || classDisplayName.isBlank()) {
            return displayName + " / " + student.getUsername();
        }
        return displayName + " / " + student.getUsername() + " / " + classDisplayName;
    }

    private Long resolveStudentId(Long studentId, List<StatisticsOptionVO> studentOptions) {
        if (studentOptions.isEmpty()) {
            return null;
        }
        if (studentId != null && studentOptions.stream().anyMatch(item -> Objects.equals(item.id(), studentId))) {
            return studentId;
        }
        return studentOptions.get(0).id();
    }

    private Map<TaskStudentKey, SubmissionDetailRow> latestSubmissionsByKey(List<SubmissionDetailRow> rows) {
        Map<TaskStudentKey, SubmissionDetailRow> result = new LinkedHashMap<>();
        for (SubmissionDetailRow row : rows) {
            TaskStudentKey key = new TaskStudentKey(row.taskId(), row.studentId());
            SubmissionDetailRow current = result.get(key);
            if (current == null
                    || compareTime(row.submittedAt(), current.submittedAt()) > 0
                    || (compareTime(row.submittedAt(), current.submittedAt()) == 0
                    && Objects.requireNonNullElse(row.versionNo(), 0) > Objects.requireNonNullElse(current.versionNo(), 0))) {
                result.put(key, row);
            }
        }
        return result;
    }

    private Map<TaskStudentKey, ReviewDetailRow> latestReviewsByKey(List<ReviewDetailRow> rows) {
        Map<TaskStudentKey, ReviewDetailRow> result = new LinkedHashMap<>();
        for (ReviewDetailRow row : rows) {
            TaskStudentKey key = new TaskStudentKey(row.taskId(), row.studentId());
            ReviewDetailRow current = result.get(key);
            if (current == null
                    || compareTime(row.reviewedAt(), current.reviewedAt()) > 0
                    || (compareTime(row.reviewedAt(), current.reviewedAt()) == 0
                    && Objects.requireNonNullElse(row.versionNo(), 0) > Objects.requireNonNullElse(current.versionNo(), 0))) {
                result.put(key, row);
            }
        }
        return result;
    }

    private Map<TaskStudentKey, CompletionDetailRow> confirmedCompletionsByKey(List<CompletionDetailRow> rows) {
        Map<TaskStudentKey, CompletionDetailRow> result = new LinkedHashMap<>();
        for (CompletionDetailRow row : rows) {
            if (!"CONFIRMED".equalsIgnoreCase(row.status())) {
                continue;
            }
            TaskStudentKey key = new TaskStudentKey(row.taskId(), row.studentId());
            CompletionDetailRow current = result.get(key);
            if (current == null || compareTime(row.confirmedAt(), current.confirmedAt()) > 0) {
                result.put(key, row);
            }
        }
        return result;
    }

    private int compareTime(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        return left.compareTo(right);
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
        if (resolvedFrom == null || resolvedTo == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "学期日期未配置完整");
        }
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "起始日期不能晚于结束日期");
        }
        return new ResolvedFilters(semester, resolvedFrom, resolvedTo);
    }

    private List<TeachingAnalyticsDashboardVO.SemesterOption> loadSemesterOptions() {
        return semesterMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SemesterEntity::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> new TeachingAnalyticsDashboardVO.SemesterOption(item.getId(), item.getName(), item.getStartDate(), item.getEndDate()))
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
        if (classId != null) {
            sql.append("""
                     AND (
                        (t.experiment_course_id IS NULL AND EXISTS (SELECT 1 FROM exp_task_target_class tc WHERE tc.task_id = t.id AND tc.class_id = :classId))
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

    private List<List<?>> buildFilterRows(AuthenticatedUser actor, TeachingAnalyticsDashboardVO.Filters filters) {
        List<List<?>> rows = new ArrayList<>();
        rows.add(row("操作者", actor.username()));
        rows.add(row("导出时间", LocalDateTime.now()));
        rows.add(row("学期", filters.semesterName()));
        rows.add(row("开始日期", filters.from()));
        rows.add(row("结束日期", filters.to()));
        if (filters.teacherId() != null) {
            String teacherLabel = filters.teachers().stream().filter(item -> Objects.equals(item.id(), filters.teacherId())).map(StatisticsOptionVO::label).findFirst().orElse(String.valueOf(filters.teacherId()));
            rows.add(row("教师", teacherLabel));
        }
        if (filters.classId() != null) {
            String classLabel = filters.classes().stream().filter(item -> Objects.equals(item.id(), filters.classId())).map(StatisticsOptionVO::label).findFirst().orElse(String.valueOf(filters.classId()));
            rows.add(row("班级", classLabel));
        }
        if (filters.studentId() != null) {
            String studentLabel = filters.students().stream().filter(item -> Objects.equals(item.id(), filters.studentId())).map(StatisticsOptionVO::label).findFirst().orElse(String.valueOf(filters.studentId()));
            rows.add(row("学生", studentLabel));
        }
        return rows;
    }

    private void recordTeacherExport(AuthenticatedUser actor, String exportType, Map<String, Object> conditions) {
        ExportRecordEntity entity = new ExportRecordEntity();
        entity.setOperatorId(actor.userId());
        entity.setExportType(exportType);
        entity.setConditionJson(toJson(conditions));
        entity.setCreatedAt(LocalDateTime.now());
        exportRecordMapper.insert(entity);
    }

    private void recordAdminExport(AuthenticatedUser actor, String section, TeachingAnalyticsDashboardVO.Filters filters) {
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
        if (filters.studentId() != null) {
            detail.put("studentId", filters.studentId());
        }
        adminAuditService.record(actor, AdminAuditActions.AUDIT_EXPORT, "teaching_analytics_report", null, detail);
    }

    private String riskLabel(BigDecimal score) {
        if (score == null) {
            return "低风险";
        }
        if (score.compareTo(BigDecimal.valueOf(plagiarismProperties.textThreshold())) >= 0) {
            return "高风险";
        }
        if (score.compareTo(BigDecimal.valueOf(0.60)) >= 0) {
            return "中风险";
        }
        return "低风险";
    }

    private String trendLabel(List<BigDecimal> scores) {
        if (scores == null || scores.size() < 2) {
            return "持平";
        }
        BigDecimal first = scores.get(0);
        BigDecimal last = scores.get(scores.size() - 1);
        if (first == null || last == null) {
            return "持平";
        }
        BigDecimal delta = last.subtract(first);
        if (delta.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return "上升";
        }
        if (delta.compareTo(BigDecimal.valueOf(-5)) <= 0) {
            return "下降";
        }
        return "持平";
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
            return ZERO_SCORE;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal maxScore(Collection<BigDecimal> scores) {
        return scores.stream().filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(ZERO_SCORE);
    }

    private BigDecimal minScore(Collection<BigDecimal> scores) {
        return scores.stream().filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(ZERO_SCORE);
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return ZERO_RATE;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private List<Object> row(Object... values) {
        List<Object> row = new ArrayList<>(values.length);
        for (Object value : values) {
            row.add(value);
        }
        return row;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
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

    private BigDecimal decimalOrNull(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bigDecimal) return bigDecimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private record SubmissionDetailRow(
            Long taskId,
            String taskTitle,
            Long teacherId,
            String teacherName,
            Long studentId,
            String studentDisplayName,
            String studentUsername,
            Long classId,
            String classDisplayName,
            Integer versionNo,
            String submitStatus,
            LocalDateTime submittedAt
    ) {
    }

    private record ReviewDetailRow(
            Long reviewId,
            Long taskId,
            String taskTitle,
            Long teacherId,
            String teacherName,
            Long studentId,
            String studentDisplayName,
            String studentUsername,
            Long classId,
            String classDisplayName,
            Integer versionNo,
            BigDecimal score,
            String comment,
            LocalDateTime reviewedAt,
            List<String> issueTags
    ) {
        private ReviewDetailRow withIssueTags(List<String> newIssueTags) {
            return new ReviewDetailRow(reviewId, taskId, taskTitle, teacherId, teacherName, studentId, studentDisplayName, studentUsername, classId, classDisplayName, versionNo, score, comment, reviewedAt, newIssueTags);
        }
    }

    private record CompletionDetailRow(
            Long taskId,
            Long studentId,
            String status,
            LocalDateTime confirmedAt
    ) {
    }

    private record PlagiarismRiskRow(
            Long taskId,
            String taskTitle,
            String teacherName,
            Long studentId,
            String studentDisplayName,
            String studentUsername,
            String classDisplayName,
            BigDecimal maxScore
    ) {
    }

    private record TaskStudentKey(
            Long taskId,
            Long studentId
    ) {
    }

    private record ExperimentComputation(
            TeachingAnalyticsDashboardVO.ExperimentAnalysis analysis
    ) {
    }

    private record StudentComputation(
            TeachingAnalyticsDashboardVO.StudentAnalysis analysis
    ) {
    }

    private record ReportQualityComputation(
            TeachingAnalyticsDashboardVO.ReportQualityAnalysis analysis
    ) {
    }

    private static final class TagAccumulator {
        private final String label;
        private long occurrenceCount;
        private final Set<Long> studentIds = new LinkedHashSet<>();
        private final List<BigDecimal> scores = new ArrayList<>();

        private TagAccumulator(String label) {
            this.label = label == null || label.isBlank() ? UNMARKED_LABEL : label;
        }
    }
}
