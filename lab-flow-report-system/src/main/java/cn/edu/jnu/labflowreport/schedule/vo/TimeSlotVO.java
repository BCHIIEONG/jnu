package cn.edu.jnu.labflowreport.schedule.vo;

import java.time.LocalTime;
import lombok.Data;

@Data
public class TimeSlotVO {
    private Long id;
    private String code;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
}

