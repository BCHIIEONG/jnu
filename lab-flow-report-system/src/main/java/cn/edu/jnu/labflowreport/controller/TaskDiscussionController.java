package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.discussion.dto.TaskDiscussionMessageCreateRequest;
import cn.edu.jnu.labflowreport.discussion.dto.TaskDiscussionThreadCreateRequest;
import cn.edu.jnu.labflowreport.discussion.service.TaskDiscussionService;
import cn.edu.jnu.labflowreport.discussion.vo.StudentDiscussionUnreadSummaryVO;
import cn.edu.jnu.labflowreport.discussion.vo.TaskDiscussionTaskViewVO;
import cn.edu.jnu.labflowreport.discussion.vo.TaskDiscussionThreadVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TaskDiscussionController {

    private final TaskDiscussionService taskDiscussionService;

    public TaskDiscussionController(TaskDiscussionService taskDiscussionService) {
        this.taskDiscussionService = taskDiscussionService;
    }

    @GetMapping("/tasks/{taskId}/discussion")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TaskDiscussionTaskViewVO> getTaskDiscussion(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(taskDiscussionService.getStudentTaskDiscussion(taskId, user));
    }

    @PostMapping("/tasks/{taskId}/discussion/threads")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TaskDiscussionThreadVO> createThread(@PathVariable Long taskId, @Valid @RequestBody TaskDiscussionThreadCreateRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已发布讨论", taskDiscussionService.createStudentThread(taskId, user, request));
    }

    @PostMapping("/tasks/{taskId}/discussion/threads/{threadId}/messages")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TaskDiscussionThreadVO> replyThread(
            @PathVariable Long taskId,
            @PathVariable Long threadId,
            @Valid @RequestBody TaskDiscussionMessageCreateRequest request
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已回复", taskDiscussionService.replyStudentThread(taskId, threadId, user, request));
    }

    @PostMapping("/tasks/{taskId}/discussion/threads/{threadId}/read")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> markThreadRead(@PathVariable Long taskId, @PathVariable Long threadId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        taskDiscussionService.markStudentThreadRead(taskId, threadId, user);
        return ApiResponse.success("已标记已读", null);
    }

    @GetMapping("/tasks/discussion/unread-summary")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentDiscussionUnreadSummaryVO> getUnreadSummary() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(taskDiscussionService.getStudentUnreadSummary(user));
    }
}
