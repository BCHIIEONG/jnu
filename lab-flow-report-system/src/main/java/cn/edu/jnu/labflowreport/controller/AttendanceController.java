package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.attendance.dto.AttendanceCheckinRequest;
import cn.edu.jnu.labflowreport.attendance.dto.AttendanceManualCheckinRequest;
import cn.edu.jnu.labflowreport.attendance.dto.AttendanceSessionCreateRequest;
import cn.edu.jnu.labflowreport.attendance.dto.AttendanceStaticCheckinRequest;
import cn.edu.jnu.labflowreport.attendance.dto.AttendanceTokenTtlUpdateRequest;
import cn.edu.jnu.labflowreport.attendance.entity.AttendanceSessionEntity;
import cn.edu.jnu.labflowreport.attendance.service.AttendanceService;
import cn.edu.jnu.labflowreport.attendance.service.AttendanceTokenService;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceRecordVO;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceSessionVO;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceTokenVO;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceTokenService tokenService;

    public AttendanceController(AttendanceService attendanceService, AttendanceTokenService tokenService) {
        this.attendanceService = attendanceService;
        this.tokenService = tokenService;
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<AttendanceSessionVO> createSession(@RequestBody AttendanceSessionCreateRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("开启成功", attendanceService.createSession(actor, request));
    }

    @PostMapping("/sessions/{sessionId}/close")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<AttendanceSessionVO> closeSession(@PathVariable Long sessionId) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("已结束", attendanceService.closeSession(actor, sessionId));
    }

    @GetMapping("/sessions/{sessionId}/token")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<AttendanceTokenVO> getToken(@PathVariable Long sessionId) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        AttendanceSessionEntity session = attendanceService.getOpenSessionOrThrow(sessionId);
        // Ensure caller has permission via the records listing check.
        attendanceService.listRecords(actor, sessionId);
        int ttl = attendanceService.getSessionTokenTtlSeconds(session);
        return ApiResponse.success(tokenService.issueToken(sessionId, ttl));
    }

    @GetMapping("/sessions/{sessionId}/static-code")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<StaticCodeResponse> getStaticCode(@PathVariable Long sessionId) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        String code = attendanceService.getOrCreateStaticCode(actor, sessionId);
        return ApiResponse.success(new StaticCodeResponse(code));
    }

    @PutMapping("/sessions/{sessionId}/token-ttl")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<AttendanceSessionVO> updateTokenTtl(
            @PathVariable Long sessionId,
            @Valid @RequestBody AttendanceTokenTtlUpdateRequest request
    ) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("已更新", attendanceService.updateSessionTokenTtl(actor, sessionId, request));
    }

    @PostMapping("/checkin")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CheckinResponse> checkIn(@Valid @RequestBody AttendanceCheckinRequest request, HttpServletRequest http) {
        AuthenticatedUser student = SecurityUtils.currentUser();
        String ip = http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");

        AttendanceService.CheckinResult result = attendanceService.checkInByToken(student, request.token(), ip, ua);
        CheckinResponse data = new CheckinResponse(result.recordId(), result.alreadyCheckedIn(), result.checkedInAt().toString());
        return ApiResponse.success(result.alreadyCheckedIn() ? "已签到" : "签到成功", data);
    }

    @PostMapping("/checkin/static")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<CheckinResponse> checkInStatic(@Valid @RequestBody AttendanceStaticCheckinRequest request, HttpServletRequest http) {
        AuthenticatedUser student = SecurityUtils.currentUser();
        String ip = http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");

        AttendanceService.CheckinResult result = attendanceService.checkInByStaticCode(student, request.code(), ip, ua);
        CheckinResponse data = new CheckinResponse(result.recordId(), result.alreadyCheckedIn(), result.checkedInAt().toString());
        return ApiResponse.success(result.alreadyCheckedIn() ? "已签到" : "签到成功", data);
    }

    @GetMapping("/sessions/{sessionId}/records")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<List<AttendanceRecordVO>> listRecords(@PathVariable Long sessionId) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success(attendanceService.listRecords(actor, sessionId));
    }

    @PostMapping("/sessions/{sessionId}/manual-checkin")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<Void> manualCheckIn(@PathVariable Long sessionId, @Valid @RequestBody AttendanceManualCheckinRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        attendanceService.manualCheckIn(actor, sessionId, request);
        return ApiResponse.success("补签成功", null);
    }

    @GetMapping("/sessions/{sessionId}/export")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(@PathVariable Long sessionId) throws Exception {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        String csv = attendanceService.exportRecordsCsv(actor, sessionId);
        String filename = "attendance-session-" + sessionId + ".csv";

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

    public record CheckinResponse(Long recordId, boolean alreadyCheckedIn, String checkedInAt) {
    }

    public record StaticCodeResponse(String code) {
    }
}
