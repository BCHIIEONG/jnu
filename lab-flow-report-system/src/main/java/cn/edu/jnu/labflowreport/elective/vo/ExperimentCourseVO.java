package cn.edu.jnu.labflowreport.elective.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.Data;

@Data
public class ExperimentCourseVO {
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private String teacherDisplayName;
    private Long semesterId;
    private String semesterName;
    private String status;
    private LocalDateTime enrollDeadlineAt;
    private List<Long> targetClassIds;
    private List<Long> targetStudentIds;
    private List<SlotVO> slots;
    private Integer totalEnrolled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class SlotVO {
        private Long id;
        private Long courseId;
        private LocalDate lessonDate;
        private Long slotId;
        private String slotCode;
        private String slotName;
        private LocalTime slotStartTime;
        private LocalTime slotEndTime;
        private Long labRoomId;
        private String labRoomName;
        private Integer capacity;
        private Integer enrolledCount;
        private Integer remainingCapacity;
    }
}
