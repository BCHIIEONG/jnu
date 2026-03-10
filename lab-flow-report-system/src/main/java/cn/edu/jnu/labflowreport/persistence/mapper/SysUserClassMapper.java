package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserClassEntity;
import cn.edu.jnu.labflowreport.schedule.vo.TeacherClassVO;
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

    @Select("""
            SELECT DISTINCT c.id,
                            CASE WHEN c.grade IS NOT NULL THEN CONCAT(c.grade, '级', c.name) ELSE c.name END AS name,
                            d.name AS department_name
            FROM sys_user_class suc
            JOIN org_class c ON c.id = suc.class_id
            LEFT JOIN org_department d ON d.id = c.department_id
            WHERE suc.user_id = #{userId}
            ORDER BY d.name ASC, c.grade ASC, c.name ASC, c.id ASC
            """)
    List<TeacherClassVO> findBoundClassesByUserId(Long userId);
}
