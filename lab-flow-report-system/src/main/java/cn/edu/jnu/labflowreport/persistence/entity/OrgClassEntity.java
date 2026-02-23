package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("org_class")
public class OrgClassEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long departmentId;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

