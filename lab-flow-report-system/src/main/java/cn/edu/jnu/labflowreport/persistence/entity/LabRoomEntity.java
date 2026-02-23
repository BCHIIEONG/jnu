package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_room")
public class LabRoomEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String location;
    private String openHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

