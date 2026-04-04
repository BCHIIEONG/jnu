package cn.edu.jnu.labflowreport.elective.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class StudentExperimentCourseVO {
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private String teacherDisplayName;
    private Long semesterId;
    private String semesterName;
    private String status;
    private LocalDateTime enrollDeadlineAt;
    private boolean enrolled;
    private Long selectedSlotId;
    private LocalDateTime selectedAt;
    private List<ExperimentCourseVO.SlotVO> slots;
}
