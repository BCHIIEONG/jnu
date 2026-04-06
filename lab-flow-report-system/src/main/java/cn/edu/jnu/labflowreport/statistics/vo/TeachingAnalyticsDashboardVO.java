package cn.edu.jnu.labflowreport.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TeachingAnalyticsDashboardVO(
        Filters filters,
        ExperimentAnalysis experimentAnalysis,
        StudentAnalysis studentAnalysis,
        ReportQualityAnalysis reportQualityAnalysis
) {

    public record Filters(
            Long semesterId,
            String semesterName,
            LocalDate from,
            LocalDate to,
            Long teacherId,
            Long classId,
            Long studentId,
            List<SemesterOption> semesters,
            List<StatisticsOptionVO> teachers,
            List<StatisticsOptionVO> classes,
            List<StatisticsOptionVO> students
    ) {
    }

    public record SemesterOption(
            Long id,
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record ExperimentAnalysis(
            StatisticsChartVO completionRateChart,
            StatisticsChartVO averageScoreChart,
            List<ExperimentTaskRow> taskTable
    ) {
    }

    public record StudentAnalysis(
            StatisticsChartVO scoreTrendChart,
            List<RiskStudentRow> riskStudentTable,
            List<WeakTaskRow> weakTaskTable
    ) {
    }

    public record ReportQualityAnalysis(
            StatisticsChartVO issueTagChart,
            StatisticsChartVO plagiarismRiskChart,
            List<IssueTagRow> issueTagTable
    ) {
    }

    public record ExperimentTaskRow(
            Long taskId,
            String taskTitle,
            String teacherName,
            long visibleStudentCount,
            long submissionCount,
            long reviewedSubmissionCount,
            long confirmedCompletionCount,
            BigDecimal completionRate,
            BigDecimal avgScore,
            BigDecimal maxScore,
            BigDecimal minScore
    ) {
    }

    public record RiskStudentRow(
            Long studentId,
            String studentDisplayName,
            String studentUsername,
            String classDisplayName,
            long submittedTaskCount,
            long reviewedTaskCount,
            BigDecimal avgScore,
            BigDecimal completionRate,
            BigDecimal recentThreeAvgScore,
            String trend,
            String riskReasons
    ) {
    }

    public record WeakTaskRow(
            Long taskId,
            String taskTitle,
            String teacherName,
            BigDecimal avgScore,
            BigDecimal completionRate,
            BigDecimal unsubmittedRate
    ) {
    }

    public record IssueTagRow(
            String tagCode,
            String tagLabel,
            long occurrenceCount,
            long studentCount,
            BigDecimal avgScore
    ) {
    }
}
