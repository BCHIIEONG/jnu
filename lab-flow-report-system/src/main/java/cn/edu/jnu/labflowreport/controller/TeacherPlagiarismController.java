package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.plagiarism.service.PlagiarismService;
import cn.edu.jnu.labflowreport.plagiarism.vo.PlagiarismRunVO;
import cn.edu.jnu.labflowreport.plagiarism.vo.PlagiarismSummaryVO;
import cn.edu.jnu.labflowreport.plagiarism.vo.PlagiarismStudentHistoryVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
public class TeacherPlagiarismController {

    private final PlagiarismService plagiarismService;

    public TeacherPlagiarismController(PlagiarismService plagiarismService) {
        this.plagiarismService = plagiarismService;
    }

    @PostMapping("/tasks/{taskId}/plagiarism/run")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<PlagiarismRunVO> run(@PathVariable Long taskId) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("已运行", plagiarismService.runForTask(actor, taskId));
    }

    @GetMapping("/submissions/{submissionId}/plagiarism-summary")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<PlagiarismSummaryVO> summary(@PathVariable Long submissionId) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success(plagiarismService.getSummary(actor, submissionId));
    }

    @GetMapping("/submissions/{submissionId}/plagiarism-history")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<PlagiarismStudentHistoryVO> history(@PathVariable Long submissionId) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success(plagiarismService.getStudentHistory(actor, submissionId));
    }
}
