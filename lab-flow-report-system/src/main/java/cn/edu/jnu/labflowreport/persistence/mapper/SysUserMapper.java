package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    @Select("SELECT id, username, password_hash, display_name, enabled, created_at, updated_at FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUserEntity findByUsername(String username);

    @Select("""
            SELECT r.code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findRoleCodesByUserId(Long userId);

    @Select("""
            SELECT id, username, display_name, class_id
            FROM sys_user
            WHERE class_id = #{classId} AND enabled = TRUE
            ORDER BY username ASC
            """)
    List<SysUserEntity> findStudentsByClassId(Long classId);
}
