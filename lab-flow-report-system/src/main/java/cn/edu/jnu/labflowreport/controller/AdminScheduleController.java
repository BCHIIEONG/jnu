package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.schedule.dto.AdminCourseScheduleRequest;
import cn.edu.jnu.labflowreport.schedule.dto.AdminTimeSlotRequest;
import cn.edu.jnu.labflowreport.schedule.service.ScheduleService;
import cn.edu.jnu.labflowreport.schedule.vo.CourseScheduleVO;
import cn.edu.jnu.labflowreport.schedule.vo.TimeSlotVO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    public AdminScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/time-slots")
    public ApiResponse<List<TimeSlotVO>> listTimeSlots() {
        return ApiResponse.success(scheduleService.listTimeSlots());
    }

    @PostMapping("/time-slots")
    public ApiResponse<TimeSlotVO> createTimeSlot(@Valid @RequestBody AdminTimeSlotRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", scheduleService.createTimeSlot(actor, request));
    }

    @PutMapping("/time-slots/{id}")
    public ApiResponse<TimeSlotVO> updateTimeSlot(@PathVariable Long id, @Valid @RequestBody AdminTimeSlotRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", scheduleService.updateTimeSlot(actor, id, request));
    }

    @DeleteMapping("/time-slots/{id}")
    public ApiResponse<Void> deleteTimeSlot(@PathVariable Long id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        scheduleService.deleteTimeSlot(actor, id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/course-schedules")
    public ApiResponse<List<CourseScheduleVO>> listCourseSchedules(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId
    ) {
        return ApiResponse.success(scheduleService.listCourseSchedules(semesterId, from, to, teacherId, classId));
    }

    @PostMapping("/course-schedules")
    public ApiResponse<CourseScheduleVO> createCourseSchedule(@Valid @RequestBody AdminCourseScheduleRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", scheduleService.createCourseSchedule(actor, request));
    }

    @PutMapping("/course-schedules/{id}")
    public ApiResponse<CourseScheduleVO> updateCourseSchedule(@PathVariable Long id, @Valid @RequestBody AdminCourseScheduleRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", scheduleService.updateCourseSchedule(actor, id, request));
    }

    @DeleteMapping("/course-schedules/{id}")
    public ApiResponse<Void> deleteCourseSchedule(@PathVariable Long id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        scheduleService.deleteCourseSchedule(actor, id);
        return ApiResponse.success("删除成功", null);
    }
}

