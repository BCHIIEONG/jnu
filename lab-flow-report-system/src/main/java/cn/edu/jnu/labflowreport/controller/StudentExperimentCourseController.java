package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.elective.dto.ExperimentCourseEnrollRequest;
import cn.edu.jnu.labflowreport.elective.service.ExperimentCourseService;
import cn.edu.jnu.labflowreport.elective.vo.StudentExperimentCourseVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExperimentCourseController {

    private final ExperimentCourseService experimentCourseService;

    public StudentExperimentCourseController(ExperimentCourseService experimentCourseService) {
        this.experimentCourseService = experimentCourseService;
    }

    @GetMapping("/experiment-courses")
    public ApiResponse<List<StudentExperimentCourseVO>> listAvailableCourses() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentCourseService.listStudentAvailableCourses(user));
    }

    @GetMapping("/experiment-courses/my")
    public ApiResponse<List<StudentExperimentCourseVO>> listMyCourses() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentCourseService.listStudentMyCourses(user));
    }

    @PostMapping("/experiment-courses/{courseId}/enroll")
    public ApiResponse<StudentExperimentCourseVO> enroll(@PathVariable Long courseId, @Valid @RequestBody ExperimentCourseEnrollRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("选课成功", experimentCourseService.enroll(user, courseId, request));
    }
}
