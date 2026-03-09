package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.schedule.service.ScheduleService;
import cn.edu.jnu.labflowreport.schedule.vo.CourseScheduleVO;
import cn.edu.jnu.labflowreport.schedule.vo.TimeSlotVO;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentScheduleController {

    private final ScheduleService scheduleService;
    private final SemesterMapper semesterMapper;

    public StudentScheduleController(ScheduleService scheduleService, SemesterMapper semesterMapper) {
        this.scheduleService = scheduleService;
        this.semesterMapper = semesterMapper;
    }

    @GetMapping("/semesters")
    public ApiResponse<List<StudentSemesterVO>> listSemesters() {
        List<SemesterEntity> list = semesterMapper.selectList(null).stream()
                .sorted(Comparator
                        .comparing(SemesterEntity::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SemesterEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ApiResponse.success(list.stream()
                .map(s -> new StudentSemesterVO(s.getId(), s.getName(), s.getStartDate(), s.getEndDate()))
                .toList());
    }

    @GetMapping("/time-slots")
    public ApiResponse<List<TimeSlotVO>> listTimeSlots() {
        return ApiResponse.success(scheduleService.listTimeSlots());
    }

    @GetMapping("/schedule/week")
    public ApiResponse<List<CourseScheduleVO>> listMyWeek(
            @RequestParam Long semesterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(scheduleService.listStudentWeek(user.userId(), semesterId, weekStartDate));
    }

    public record StudentSemesterVO(Long id, String name, LocalDate startDate, LocalDate endDate) {
    }
}
