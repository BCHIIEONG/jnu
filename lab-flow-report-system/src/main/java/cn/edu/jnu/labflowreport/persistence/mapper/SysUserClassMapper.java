package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserClassEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserClassMapper extends BaseMapper<SysUserClassEntity> {

    @Select("""
            SELECT class_id
            FROM sys_user_class
            WHERE user_id = #{userId}
            ORDER BY class_id ASC
            """)
    List<Long> findClassIdsByUserId(Long userId);

    @Select("""
            SELECT DISTINCT user_id
            FROM sys_user_class
            WHERE class_id = #{classId}
            """)
    List<Long> findUserIdsByClassId(Long classId);
}
