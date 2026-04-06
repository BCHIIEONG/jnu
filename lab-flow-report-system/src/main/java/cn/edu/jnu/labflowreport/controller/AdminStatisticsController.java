package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.statistics.service.StatisticsService;
import cn.edu.jnu.labflowreport.statistics.vo.AdminStatisticsDashboardVO;
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
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    public AdminStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminStatisticsDashboardVO> getDashboard(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(statisticsService.getAdminDashboard(user, semesterId, from, to, teacherId, classId));
    }

    @GetMapping("/reports/teachers/export")
    public ResponseEntity<byte[]> exportTeachers(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId
    ) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return csvResponse("admin-teacher-stats.csv", statisticsService.exportAdminTeacherStatsCsv(user, semesterId, from, to, teacherId, classId));
    }

    @GetMapping("/reports/classes/export")
    public ResponseEntity<byte[]> exportClasses(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId
    ) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return csvResponse("admin-class-stats.csv", statisticsService.exportAdminClassStatsCsv(user, semesterId, from, to, teacherId, classId));
    }

    @GetMapping("/reports/experiment-courses/export")
    public ResponseEntity<byte[]> exportExperimentCourses(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId
    ) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return csvResponse("admin-experiment-course-stats.csv", statisticsService.exportAdminExperimentCourseStatsCsv(user, semesterId, from, to, teacherId, classId));
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
