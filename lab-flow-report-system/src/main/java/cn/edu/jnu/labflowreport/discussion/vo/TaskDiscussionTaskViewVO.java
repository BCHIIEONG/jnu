package cn.edu.jnu.labflowreport.discussion.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TaskDiscussionTaskViewVO {
    private List<TaskDiscussionThreadVO> teacherQaThreads = new ArrayList<>();
    private List<TaskDiscussionThreadVO> normalThreads = new ArrayList<>();
    private int unreadTeacherReplyCount;
}
