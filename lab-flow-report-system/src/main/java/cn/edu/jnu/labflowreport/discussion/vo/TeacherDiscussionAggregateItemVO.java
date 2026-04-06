package cn.edu.jnu.labflowreport.discussion.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TeacherDiscussionAggregateItemVO {
    private Long threadId;
    private Long taskId;
    private String taskTitle;
    private Long studentId;
    private String studentUsername;
    private String studentDisplayName;
    private String latestMessagePreview;
    private String latestTeacherReplyPreview;
    private LocalDateTime latestMessageAt;
    private int unreadCount;
}
