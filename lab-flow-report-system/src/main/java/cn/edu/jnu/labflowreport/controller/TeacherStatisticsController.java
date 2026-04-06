package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.statistics.service.StatisticsService;
import cn.edu.jnu.labflowreport.statistics.vo.TeacherStatisticsDashboardVO;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        return csvResponse("teacher-task-stats.csv", statisticsService.exportTeacherTaskStatsCsv(user, semesterId, from, to));
    }

    @GetMapping("/reports/experiment-courses/export")
    public ResponseEntity<byte[]> exportExperimentCourses(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return csvResponse("teacher-experiment-course-stats.csv", statisticsService.exportTeacherExperimentCourseStatsCsv(user, semesterId, from, to));
    }

    @GetMapping("/reports/device-requests/export")
    public ResponseEntity<byte[]> exportDeviceRequests(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return csvResponse("teacher-device-request-stats.csv", statisticsService.exportTeacherDeviceRequestStatsCsv(user, semesterId, from, to));
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String csv) throws Exception {
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(csvBytes.length + 3);
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        out.write(csvBytes);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(out.toByteArray());
    }
}
