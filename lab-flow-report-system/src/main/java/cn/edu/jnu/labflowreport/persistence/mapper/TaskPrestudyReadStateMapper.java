package cn.edu.jnu.labflowreport.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.edu.jnu.labflowreport.persistence.entity.TaskPrestudyReadStateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskPrestudyReadStateMapper extends BaseMapper<TaskPrestudyReadStateEntity> {

    @Select("""
            SELECT id, prestudy_id, student_id, last_read_version, last_read_at
            FROM task_prestudy_read_state
            WHERE prestudy_id = #{prestudyId}
              AND student_id = #{studentId}
            """)
    TaskPrestudyReadStateEntity findByPrestudyAndStudent(Long prestudyId, Long studentId);
}
