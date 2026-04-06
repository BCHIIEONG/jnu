package cn.edu.jnu.labflowreport.discussion.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TaskDiscussionMessageVO {
    private Long id;
    private Long threadId;
    private Long authorId;
    private String authorUsername;
    private String authorDisplayName;
    private String authorRole;
    private String content;
    private LocalDateTime createdAt;
}
