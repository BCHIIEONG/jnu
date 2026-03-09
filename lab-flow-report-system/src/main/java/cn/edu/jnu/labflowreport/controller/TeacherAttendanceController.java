package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.admin.dto.PageResult;
import cn.edu.jnu.labflowreport.attendance.service.AttendanceService;
import cn.edu.jnu.labflowreport.attendance.vo.TeacherAttendanceSessionDetailVO;
import cn.edu.jnu.labflowreport.attendance.vo.TeacherAttendanceSessionListItemVO;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/attendance")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherAttendanceController {

    private final AttendanceService attendanceService;

    public TeacherAttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResult<TeacherAttendanceSessionListItemVO>> listSessions(
            @RequestParam(required = false) Integer grade,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String roomKeyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success(attendanceService.listTeacherSessions(actor, grade, classId, roomKeyword, from, to, status, page, size));
    }

    @GetMapping("/sessions/{sessionId}/detail")
    public ApiResponse<TeacherAttendanceSessionDetailVO> getSessionDetail(@PathVariable Long sessionId) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success(attendanceService.getTeacherSessionDetail(actor, sessionId));
    }
}
