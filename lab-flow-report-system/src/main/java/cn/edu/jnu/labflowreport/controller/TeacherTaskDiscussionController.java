package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.discussion.dto.TaskDiscussionMessageCreateRequest;
import cn.edu.jnu.labflowreport.discussion.service.TaskDiscussionService;
import cn.edu.jnu.labflowreport.discussion.vo.TaskDiscussionTaskViewVO;
import cn.edu.jnu.labflowreport.discussion.vo.TaskDiscussionThreadVO;
import cn.edu.jnu.labflowreport.discussion.vo.TeacherDiscussionAggregateItemVO;
import cn.edu.jnu.labflowreport.discussion.vo.UnreadCountVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherTaskDiscussionController {

    private final TaskDiscussionService taskDiscussionService;

    public TeacherTaskDiscussionController(TaskDiscussionService taskDiscussionService) {
        this.taskDiscussionService = taskDiscussionService;
    }

    @GetMapping("/discussions")
    public ApiResponse<List<TeacherDiscussionAggregateItemVO>> listDiscussions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(taskDiscussionService.getTeacherDiscussionAggregate(user, q, taskId, unreadOnly));
    }

    @GetMapping("/discussions/unread-summary")
    public ApiResponse<UnreadCountVO> getUnreadSummary() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(taskDiscussionService.getTeacherUnreadSummary(user));
    }

    @GetMapping("/tasks/{taskId}/discussion")
    public ApiResponse<TaskDiscussionTaskViewVO> getTaskDiscussion(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(taskDiscussionService.getTeacherTaskDiscussion(taskId, user));
    }

    @PostMapping("/tasks/{taskId}/discussion/threads/{threadId}/messages")
    public ApiResponse<TaskDiscussionThreadVO> replyThread(
            @PathVariable Long taskId,
            @PathVariable Long threadId,
            @Valid @RequestBody TaskDiscussionMessageCreateRequest request
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已回复", taskDiscussionService.replyTeacherThread(taskId, threadId, user, request));
    }

    @PostMapping("/tasks/{taskId}/discussion/threads/{threadId}/read")
    public ApiResponse<Void> markThreadRead(@PathVariable Long taskId, @PathVariable Long threadId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        taskDiscussionService.markTeacherThreadRead(taskId, threadId, user);
        return ApiResponse.success("已标记已读", null);
    }
}
