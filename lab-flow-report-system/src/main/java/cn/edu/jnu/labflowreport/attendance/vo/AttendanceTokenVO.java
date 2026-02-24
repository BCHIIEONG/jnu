package cn.edu.jnu.labflowreport.attendance.vo;

import lombok.Data;

@Data
public class AttendanceTokenVO {
    private String token;
    private long issuedAtEpochSec;
    private int ttlSeconds;
}

