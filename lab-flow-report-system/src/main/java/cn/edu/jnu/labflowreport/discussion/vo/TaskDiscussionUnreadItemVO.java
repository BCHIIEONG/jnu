package cn.edu.jnu.labflowreport.discussion.vo;

import lombok.Data;

@Data
public class TaskDiscussionUnreadItemVO {
    private Long taskId;
    private int unreadCount;
}
