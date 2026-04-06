package cn.edu.jnu.labflowreport.discussion.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TaskDiscussionThreadVO {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private String type;
    private Long creatorId;
    private String creatorUsername;
    private String creatorDisplayName;
    private String latestMessagePreview;
    private LocalDateTime latestMessageAt;
    private String latestTeacherReplyPreview;
    private LocalDateTime latestTeacherReplyAt;
    private int unreadCount;
    private List<TaskDiscussionMessageVO> messages = new ArrayList<>();
}
