package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgDepartmentEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgDepartmentMapper;
import cn.edu.jnu.labflowreport.schedule.mapper.CourseScheduleMapper;
import cn.edu.jnu.labflowreport.schedule.vo.TeacherClassVO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherMetaController {

    private final CourseScheduleMapper courseScheduleMapper;
    private final OrgClassMapper orgClassMapper;
    private final OrgDepartmentMapper orgDepartmentMapper;

    public TeacherMetaController(
            CourseScheduleMapper courseScheduleMapper,
            OrgClassMapper orgClassMapper,
            OrgDepartmentMapper orgDepartmentMapper
    ) {
        this.courseScheduleMapper = courseScheduleMapper;
        this.orgClassMapper = orgClassMapper;
        this.orgDepartmentMapper = orgDepartmentMapper;
    }

    @GetMapping("/classes")
    public ApiResponse<List<TeacherClassVO>> listClasses(@RequestParam(required = false, defaultValue = "mine") String scope) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        String s = (scope == null ? "mine" : scope.trim().toLowerCase(java.util.Locale.ROOT));
        if (!s.equals("mine") && !s.equals("all")) {
            s = "mine";
        }

        if (s.equals("mine") && actor.roleCodes().contains("ROLE_TEACHER")) {
            return ApiResponse.success(courseScheduleMapper.findDistinctClassesForTeacher(actor.userId()));
        }

        // all classes (or admin "mine")
        List<OrgClassEntity> classes = orgClassMapper.selectList(null);
        Set<Long> depIds = classes.stream().map(OrgClassEntity::getDepartmentId).filter(x -> x != null).collect(Collectors.toSet());
        Map<Long, String> depNames = depIds.isEmpty()
                ? Map.of()
                : orgDepartmentMapper.selectBatchIds(depIds).stream()
                .collect(Collectors.toMap(OrgDepartmentEntity::getId, d -> String.valueOf(d.getName())));

        List<TeacherClassVO> out = classes.stream()
                .map(c -> new TeacherClassVO(c.getId(), c.getName(), depNames.get(c.getDepartmentId())))
                .sorted((a, b) -> {
                    String da = a.departmentName() == null ? "" : a.departmentName();
                    String db = b.departmentName() == null ? "" : b.departmentName();
                    int c1 = da.compareTo(db);
                    if (c1 != 0) return c1;
                    String na = a.name() == null ? "" : a.name();
                    String nb = b.name() == null ? "" : b.name();
                    int c2 = na.compareTo(nb);
                    if (c2 != 0) return c2;
                    long ia = a.id() == null ? 0 : a.id();
                    long ib = b.id() == null ? 0 : b.id();
                    return Long.compare(ia, ib);
                })
                .toList();

        return ApiResponse.success(out);
    }
}

