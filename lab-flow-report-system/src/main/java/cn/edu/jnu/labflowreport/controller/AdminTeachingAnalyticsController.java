package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.common.export.ExportResponseHelper;
import cn.edu.jnu.labflowreport.statistics.service.TeachingAnalyticsService;
import cn.edu.jnu.labflowreport.statistics.vo.TeachingAnalyticsDashboardVO;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTeachingAnalyticsController {

    private final TeachingAnalyticsService teachingAnalyticsService;

    public AdminTeachingAnalyticsController(TeachingAnalyticsService teachingAnalyticsService) {
        this.teachingAnalyticsService = teachingAnalyticsService;
    }

    @GetMapping("/teaching")
    public ApiResponse<TeachingAnalyticsDashboardVO> getDashboard(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(teachingAnalyticsService.getAdminAnalytics(user, semesterId, from, to, teacherId, classId, studentId));
    }

    @GetMapping("/teaching/experiment/export/excel")
    public ResponseEntity<byte[]> exportExperimentExcel(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx(
                "admin-teaching-experiment-analytics.xlsx",
                teachingAnalyticsService.exportAdminExperimentAnalyticsExcel(user, semesterId, from, to, teacherId, classId, studentId)
        );
    }

    @GetMapping("/teaching/student/export/excel")
    public ResponseEntity<byte[]> exportStudentExcel(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx(
                "admin-teaching-student-analytics.xlsx",
                teachingAnalyticsService.exportAdminStudentAnalyticsExcel(user, semesterId, from, to, teacherId, classId, studentId)
        );
    }

    @GetMapping("/teaching/report-quality/export/excel")
    public ResponseEntity<byte[]> exportReportQualityExcel(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx(
                "admin-teaching-report-quality-analytics.xlsx",
                teachingAnalyticsService.exportAdminReportQualityAnalyticsExcel(user, semesterId, from, to, teacherId, classId, studentId)
        );
    }
}
