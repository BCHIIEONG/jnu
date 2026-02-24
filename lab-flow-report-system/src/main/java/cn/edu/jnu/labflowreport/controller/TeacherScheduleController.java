package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.schedule.service.ScheduleService;
import cn.edu.jnu.labflowreport.schedule.vo.TeacherWeekScheduleItemVO;
import cn.edu.jnu.labflowreport.schedule.vo.TimeSlotVO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherScheduleController {

    private final ScheduleService scheduleService;
    private final SysUserMapper sysUserMapper;
    private final SemesterMapper semesterMapper;

    public TeacherScheduleController(
            ScheduleService scheduleService,
            SysUserMapper sysUserMapper,
            SemesterMapper semesterMapper
    ) {
        this.scheduleService = scheduleService;
        this.sysUserMapper = sysUserMapper;
        this.semesterMapper = semesterMapper;
    }

    @GetMapping("/semesters")
    public ApiResponse<List<TeacherSemesterVO>> listSemesters() {
        List<SemesterEntity> list = semesterMapper.selectList(null);
        return ApiResponse.success(list.stream()
                .map(s -> new TeacherSemesterVO(s.getId(), s.getName(), s.getStartDate(), s.getEndDate()))
                .toList());
    }

    @GetMapping("/time-slots")
    public ApiResponse<List<TimeSlotVO>> listTimeSlots() {
        return ApiResponse.success(scheduleService.listTimeSlots());
    }

    @GetMapping("/schedule/week")
    public ApiResponse<List<TeacherWeekScheduleItemVO>> listMyWeek(
            @RequestParam Long semesterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(scheduleService.listTeacherWeek(user.userId(), semesterId, weekStartDate));
    }

    @GetMapping("/classes/{classId}/roster")
    public ApiResponse<List<TeacherStudentVO>> getClassRoster(@PathVariable Long classId) {
        return ApiResponse.success(sysUserMapper.findStudentsByClassId(classId).stream()
                .map(u -> new TeacherStudentVO(u.getId(), u.getUsername(), u.getDisplayName()))
                .toList());
    }

    public record TeacherStudentVO(Long id, String username, String displayName) {
    }

    public record TeacherSemesterVO(Long id, String name, LocalDate startDate, LocalDate endDate) {
    }
}
