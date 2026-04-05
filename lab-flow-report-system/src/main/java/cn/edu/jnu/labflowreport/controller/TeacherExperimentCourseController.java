package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.elective.dto.ExperimentCourseSaveRequest;
import cn.edu.jnu.labflowreport.elective.dto.ExperimentCourseStatusUpdateRequest;
import cn.edu.jnu.labflowreport.elective.dto.TeacherExperimentCourseManualEnrollRequest;
import cn.edu.jnu.labflowreport.elective.dto.TeacherExperimentCourseRemoveStudentRequest;
import cn.edu.jnu.labflowreport.elective.service.ExperimentCourseService;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseRosterVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseEnrollmentRowVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseStudentOptionVO;
import cn.edu.jnu.labflowreport.elective.vo.ExperimentCourseVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherExperimentCourseController {

    private final ExperimentCourseService experimentCourseService;

    public TeacherExperimentCourseController(ExperimentCourseService experimentCourseService) {
        this.experimentCourseService = experimentCourseService;
    }

    @GetMapping("/experiment-courses")
    public ApiResponse<List<ExperimentCourseVO>> listCourses() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentCourseService.listTeacherCourses(user));
    }

    @PostMapping("/experiment-courses")
    public ApiResponse<ExperimentCourseVO> createCourse(@Valid @RequestBody ExperimentCourseSaveRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("实验课程创建成功", experimentCourseService.createTeacherCourse(user, request));
    }

    @PutMapping("/experiment-courses/{courseId}")
    public ApiResponse<ExperimentCourseVO> updateCourse(@PathVariable Long courseId, @Valid @RequestBody ExperimentCourseSaveRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("实验课程已更新", experimentCourseService.updateTeacherCourse(courseId, user, request));
    }

    @PutMapping("/experiment-courses/{courseId}/status")
    public ApiResponse<ExperimentCourseVO> updateCourseStatus(@PathVariable Long courseId, @Valid @RequestBody ExperimentCourseStatusUpdateRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("实验课程状态已更新", experimentCourseService.updateTeacherCourseStatus(courseId, user, request.status()));
    }

    @GetMapping("/experiment-courses/{courseId}/enrollments")
    public ApiResponse<List<ExperimentCourseEnrollmentRowVO>> listEnrollments(@PathVariable Long courseId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentCourseService.listTeacherEnrollments(courseId, user));
    }

    @GetMapping("/experiment-courses/{courseId}/roster")
    public ApiResponse<ExperimentCourseRosterVO> getRoster(@PathVariable Long courseId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentCourseService.getTeacherCourseRoster(courseId, user));
    }

    @PostMapping("/experiment-courses/{courseId}/roster/enroll")
    public ApiResponse<ExperimentCourseRosterVO> enrollStudent(
            @PathVariable Long courseId,
            @Valid @RequestBody TeacherExperimentCourseManualEnrollRequest request
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("学生已加入实验课程", experimentCourseService.teacherEnrollStudent(courseId, user, request));
    }

    @PostMapping("/experiment-courses/{courseId}/roster/remove")
    public ApiResponse<ExperimentCourseRosterVO> removeStudent(
            @PathVariable Long courseId,
            @Valid @RequestBody TeacherExperimentCourseRemoveStudentRequest request
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("学生已移出实验课程", experimentCourseService.teacherRemoveStudent(courseId, user, request));
    }

    @DeleteMapping("/experiment-courses/{courseId}/blocked-students/{studentId}")
    public ApiResponse<ExperimentCourseRosterVO> unblockStudent(@PathVariable Long courseId, @PathVariable Long studentId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已解除禁选", experimentCourseService.unblockStudent(courseId, studentId, user));
    }

    @GetMapping("/experiment-courses/student-options")
    public ApiResponse<List<ExperimentCourseStudentOptionVO>> listStudentOptions(@RequestParam(required = false) String q) {
        return ApiResponse.success(experimentCourseService.listStudentOptions(q));
    }

    @GetMapping("/experiment-course-slots/{slotId}/roster")
    public ApiResponse<List<ExperimentCourseStudentOptionVO>> listSlotRoster(@PathVariable Long slotId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentCourseService.listTeacherSlotRoster(slotId, user));
    }

    @GetMapping("/experiment-courses/meta")
    public ApiResponse<ExperimentCourseService.MetaVO> getMeta() {
        return ApiResponse.success(experimentCourseService.getMeta());
    }
}
