package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.persistence.entity.AuditLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {}

