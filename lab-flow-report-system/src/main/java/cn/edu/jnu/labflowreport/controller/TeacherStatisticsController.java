package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.common.export.ExportResponseHelper;
import cn.edu.jnu.labflowreport.statistics.service.StatisticsService;
import cn.edu.jnu.labflowreport.statistics.vo.TeacherStatisticsDashboardVO;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/statistics")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherStatisticsController {

    private final StatisticsService statisticsService;

    public TeacherStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<TeacherStatisticsDashboardVO> getDashboard(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(statisticsService.getTeacherDashboard(user, semesterId, from, to));
    }

    @GetMapping("/reports/tasks/export")
    public ResponseEntity<byte[]> exportTasks(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.csv("teacher-task-stats.csv", statisticsService.exportTeacherTaskStatsCsv(user, semesterId, from, to));
    }

    @GetMapping("/reports/experiment-courses/export")
    public ResponseEntity<byte[]> exportExperimentCourses(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.csv("teacher-experiment-course-stats.csv", statisticsService.exportTeacherExperimentCourseStatsCsv(user, semesterId, from, to));
    }

    @GetMapping("/reports/device-requests/export")
    public ResponseEntity<byte[]> exportDeviceRequests(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.csv("teacher-device-request-stats.csv", statisticsService.exportTeacherDeviceRequestStatsCsv(user, semesterId, from, to));
    }

    @GetMapping("/reports/tasks/export/excel")
    public ResponseEntity<byte[]> exportTasksExcel(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx("teacher-task-stats.xlsx", statisticsService.exportTeacherTaskStatsExcel(user, semesterId, from, to));
    }

    @GetMapping("/reports/experiment-courses/export/excel")
    public ResponseEntity<byte[]> exportExperimentCoursesExcel(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx("teacher-experiment-course-stats.xlsx", statisticsService.exportTeacherExperimentCourseStatsExcel(user, semesterId, from, to));
    }

    @GetMapping("/reports/device-requests/export/excel")
    public ResponseEntity<byte[]> exportDeviceRequestsExcel(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx("teacher-device-request-stats.xlsx", statisticsService.exportTeacherDeviceRequestStatsExcel(user, semesterId, from, to));
    }
}
