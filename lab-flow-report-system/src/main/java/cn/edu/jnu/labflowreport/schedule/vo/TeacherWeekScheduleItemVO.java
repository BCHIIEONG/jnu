package cn.edu.jnu.labflowreport.schedule.vo;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

@Data
public class TeacherWeekScheduleItemVO {
    private Long id;
    private Long semesterId;
    private Long classId;
    private String className;
    private Long teacherId;
    private String teacherDisplayName;
    private Long labRoomId;
    private String labRoomName;
    private LocalDate lessonDate;
    private Long slotId;
    private String slotCode;
    private String slotName;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private String courseName;
}

