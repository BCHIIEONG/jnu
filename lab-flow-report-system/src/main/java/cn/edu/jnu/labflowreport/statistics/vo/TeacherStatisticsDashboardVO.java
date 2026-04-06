package cn.edu.jnu.labflowreport.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TeacherStatisticsDashboardVO(
        Filters filters,
        Summary summary,
        Charts charts,
        Tables tables
) {

    public record Filters(
            Long semesterId,
            String semesterName,
            LocalDate from,
            LocalDate to,
            List<SemesterOption> semesters
    ) {
    }

    public record SemesterOption(
            Long id,
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record Summary(
            long taskCount,
            long openTaskCount,
            long submissionCount,
            long reviewedSubmissionCount,
            BigDecimal avgScore,
            long confirmedCompletionCount,
            long attendanceSessionCount,
            BigDecimal avgAttendanceRate,
            long experimentCourseCount,
            long activeEnrollmentCount,
            long pendingDeviceRequestCount
    ) {
    }

    public record Charts(
            StatisticsChartVO taskTrend,
            StatisticsChartVO attendanceTrend
    ) {
    }

    public record Tables(
            List<TaskRow> taskTable,
            List<ExperimentCourseRow> experimentCourseTable,
            List<DeviceRequestRow> deviceRequestTable
    ) {
    }

    public record TaskRow(
            Long taskId,
            String taskTitle,
            long submissionCount,
            long reviewedSubmissionCount,
            BigDecimal avgScore,
            long confirmedCompletionCount
    ) {
    }

    public record ExperimentCourseRow(
            Long courseId,
            String courseTitle,
            long slotCount,
            long activeEnrollmentCount,
            long attendanceSessionCount
    ) {
    }

    public record DeviceRequestRow(
            Long taskId,
            String taskTitle,
            long pendingCount,
            long approvedCount,
            long borrowedCount,
            long returnedCount
    ) {
    }
}
