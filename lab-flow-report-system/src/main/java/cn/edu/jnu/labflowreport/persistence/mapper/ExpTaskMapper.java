package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.ExpTaskEntity;
import cn.edu.jnu.labflowreport.workflow.vo.TaskVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExpTaskMapper extends BaseMapper<ExpTaskEntity> {

    @Select("""
            SELECT t.id,
                   t.title,
                   t.description,
                   t.publisher_id,
                   u.display_name AS publisher_name,
                   t.deadline_at,
                   t.status,
                   t.created_at
            FROM exp_task t
            JOIN sys_user u ON u.id = t.publisher_id
            ORDER BY t.created_at DESC
            """)
    List<TaskVO> findTaskList();

    @Select("""
            SELECT t.id,
                   t.title,
                   t.description,
                   t.publisher_id,
                   u.display_name AS publisher_name,
                   t.deadline_at,
                   t.status,
                   t.created_at
            FROM exp_task t
            JOIN sys_user u ON u.id = t.publisher_id
            WHERE t.id = #{taskId}
            """)
    TaskVO findTaskById(Long taskId);
}

