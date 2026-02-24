package cn.edu.jnu.labflowreport.schedule.mapper;

import cn.edu.jnu.labflowreport.schedule.entity.TimeSlotEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TimeSlotMapper extends BaseMapper<TimeSlotEntity> {

    @Select("""
            SELECT id, code, name, start_time, end_time, created_at, updated_at
            FROM time_slot
            ORDER BY start_time ASC, id ASC
            """)
    List<TimeSlotEntity> findAllOrdered();
}

