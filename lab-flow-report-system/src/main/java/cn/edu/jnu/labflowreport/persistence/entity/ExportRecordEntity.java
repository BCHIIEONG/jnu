package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("export_record")
public class ExportRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String exportType;
    private String conditionJson;
    private LocalDateTime createdAt;
}

