package cn.edu.jnu.labflowreport.discussion.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class StudentDiscussionUnreadSummaryVO {
    private int totalUnreadCount;
    private List<TaskDiscussionUnreadItemVO> items = new ArrayList<>();
}
