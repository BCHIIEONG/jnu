package cn.edu.jnu.labflowreport.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminStatisticsDashboardVO(
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
            Long teacherId,
            Long classId,
            List<SemesterOption> semesters,
            List<StatisticsOptionVO> teachers,
            List<StatisticsOptionVO> classes
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
            long teacherCount,
            long studentCount,
            long classCount,
            long taskCount,
            long experimentCourseCount,
            long submissionCount,
            long reviewedSubmissionCount,
            BigDecimal avgScore,
            long attendanceSessionCount,
            BigDecimal avgAttendanceRate,
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
            List<TeacherRow> teacherTable,
            List<ClassRow> classTable,
            List<ExperimentCourseRow> experimentCourseTable
    ) {
    }

    public record TeacherRow(
            Long teacherId,
            String teacherName,
            long taskCount,
            long submissionCount,
            long reviewedSubmissionCount,
            long attendanceSessionCount,
            BigDecimal avgAttendanceRate
    ) {
    }

    public record ClassRow(
            Long classId,
            String className,
            long studentCount,
            long submissionCount,
            long attendanceSessionCount,
            BigDecimal avgAttendanceRate
    ) {
    }

    public record ExperimentCourseRow(
            Long courseId,
            String courseTitle,
            Long teacherId,
            String teacherName,
            long activeEnrollmentCount,
            long slotCount,
            long attendanceSessionCount
    ) {
    }
}
