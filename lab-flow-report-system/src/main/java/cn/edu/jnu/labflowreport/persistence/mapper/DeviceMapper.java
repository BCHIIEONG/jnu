package cn.edu.jnu.labflowreport.persistence.mapper;

import cn.edu.jnu.labflowreport.persistence.entity.DeviceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceMapper extends BaseMapper<DeviceEntity> {

    @Select("""
            SELECT id, code, name, status, total_quantity, location, description, created_at, updated_at
            FROM device
            WHERE id = #{id}
            FOR UPDATE
            """)
    DeviceEntity findByIdForUpdate(Long id);
}
