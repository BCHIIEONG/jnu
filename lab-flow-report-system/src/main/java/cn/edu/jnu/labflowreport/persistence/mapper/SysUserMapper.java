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
            SELECT DISTINCT su.id, su.username, su.display_name, su.class_id
            FROM sys_user su
            JOIN sys_user_role ur ON ur.user_id = su.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE su.class_id = #{classId}
              AND su.enabled = TRUE
              AND r.code = 'ROLE_STUDENT'
            ORDER BY su.username ASC
            """)
    List<SysUserEntity> findStudentsByClassId(Long classId);

    @Select("""
            SELECT DISTINCT su.id, su.username, su.display_name, su.enabled, su.department_id, su.class_id, su.created_at, su.updated_at
            FROM sys_user su
            JOIN sys_user_role ur ON ur.user_id = su.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE su.enabled = TRUE
              AND r.code = 'ROLE_STUDENT'
              AND (
                    NOT EXISTS (SELECT 1 FROM exp_task_target_class tc WHERE tc.task_id = #{taskId})
                    OR su.class_id IN (SELECT tc.class_id FROM exp_task_target_class tc WHERE tc.task_id = #{taskId})
              )
            ORDER BY su.username ASC
            """)
    List<SysUserEntity> findStudentsForTask(Long taskId);

    @Select("""
            SELECT DISTINCT su.id, su.username, su.password_hash, su.display_name, su.enabled, su.department_id, su.class_id, su.created_at, su.updated_at
            FROM sys_user su
            JOIN sys_user_role ur ON ur.user_id = su.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE su.enabled = TRUE
              AND r.code = 'ROLE_TEACHER'
            ORDER BY CASE WHEN su.username = 'teacher' THEN 0 ELSE 1 END, su.id ASC
            LIMIT 1
            """)
    SysUserEntity findPrimaryTeacher();
}
