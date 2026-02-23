package cn.edu.jnu.labflowreport.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.edu.jnu.labflowreport.admin.dto.AdminClassRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminClassVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminDepartmentRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminDepartmentVO;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgDepartmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgDepartmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminOrgService {

    private final OrgDepartmentMapper orgDepartmentMapper;
    private final OrgClassMapper orgClassMapper;
    private final SysUserMapper sysUserMapper;
    private final AdminAuditService adminAuditService;

    public AdminOrgService(
            OrgDepartmentMapper orgDepartmentMapper,
            OrgClassMapper orgClassMapper,
            SysUserMapper sysUserMapper,
            AdminAuditService adminAuditService
    ) {
        this.orgDepartmentMapper = orgDepartmentMapper;
        this.orgClassMapper = orgClassMapper;
        this.sysUserMapper = sysUserMapper;
        this.adminAuditService = adminAuditService;
    }

    public List<AdminDepartmentVO> listDepartments() {
        return orgDepartmentMapper.selectList(new LambdaQueryWrapper<OrgDepartmentEntity>().orderByAsc(OrgDepartmentEntity::getId))
                .stream()
                .map(d -> new AdminDepartmentVO(d.getId(), d.getName(), d.getCreatedAt(), d.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public AdminDepartmentVO createDepartment(AuthenticatedUser actor, AdminDepartmentRequest request) {
        String name = request.name().trim();
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, "name 不能为空");
        }
        OrgDepartmentEntity exists = orgDepartmentMapper.selectOne(new LambdaQueryWrapper<OrgDepartmentEntity>()
                .eq(OrgDepartmentEntity::getName, name)
                .last("LIMIT 1"));
        if (exists != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "院系已存在");
        }
        OrgDepartmentEntity entity = new OrgDepartmentEntity();
        entity.setName(name);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        orgDepartmentMapper.insert(entity);
        adminAuditService.record(actor, AdminAuditActions.DEPARTMENT_CREATE, "org_department", entity.getId(), Map.of("name", name));
        return new AdminDepartmentVO(entity.getId(), entity.getName(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Transactional
    public AdminDepartmentVO updateDepartment(AuthenticatedUser actor, Long id, AdminDepartmentRequest request) {
        OrgDepartmentEntity existing = orgDepartmentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "院系不存在");
        }
        String name = request.name().trim();
        OrgDepartmentEntity dup = orgDepartmentMapper.selectOne(new LambdaQueryWrapper<OrgDepartmentEntity>()
                .eq(OrgDepartmentEntity::getName, name)
                .ne(OrgDepartmentEntity::getId, id)
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "院系名称已被占用");
        }
        orgDepartmentMapper.update(null, new LambdaUpdateWrapper<OrgDepartmentEntity>()
                .eq(OrgDepartmentEntity::getId, id)
                .set(OrgDepartmentEntity::getName, name)
                .set(OrgDepartmentEntity::getUpdatedAt, LocalDateTime.now()));
        adminAuditService.record(actor, AdminAuditActions.DEPARTMENT_UPDATE, "org_department", id, Map.of("name", name));
        OrgDepartmentEntity updated = orgDepartmentMapper.selectById(id);
        return new AdminDepartmentVO(updated.getId(), updated.getName(), updated.getCreatedAt(), updated.getUpdatedAt());
    }

    @Transactional
    public void deleteDepartment(AuthenticatedUser actor, Long id) {
        OrgDepartmentEntity existing = orgDepartmentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "院系不存在");
        }
        long classCount = orgClassMapper.selectCount(new LambdaQueryWrapper<OrgClassEntity>().eq(OrgClassEntity::getDepartmentId, id));
        if (classCount > 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "该院系下仍有班级，禁止删除");
        }
        long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getDepartmentId, id));
        if (userCount > 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "仍有用户归属该院系，禁止删除");
        }
        orgDepartmentMapper.deleteById(id);
        adminAuditService.record(actor, AdminAuditActions.DEPARTMENT_DELETE, "org_department", id, Map.of("name", existing.getName()));
    }

    public List<AdminClassVO> listClasses(Long departmentId) {
        LambdaQueryWrapper<OrgClassEntity> w = new LambdaQueryWrapper<OrgClassEntity>().orderByAsc(OrgClassEntity::getId);
        if (departmentId != null) {
            w.eq(OrgClassEntity::getDepartmentId, departmentId);
        }
        List<OrgClassEntity> classes = orgClassMapper.selectList(w);
        Set<Long> depIds = classes.stream().map(OrgClassEntity::getDepartmentId).collect(Collectors.toSet());
        Map<Long, String> depNames = depIds.isEmpty() ? Map.of() : orgDepartmentMapper.selectBatchIds(depIds).stream()
                .collect(Collectors.toMap(OrgDepartmentEntity::getId, OrgDepartmentEntity::getName));
        return classes.stream()
                .map(c -> new AdminClassVO(c.getId(), c.getDepartmentId(), depNames.get(c.getDepartmentId()), c.getName(), c.getCreatedAt(), c.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public AdminClassVO createClass(AuthenticatedUser actor, AdminClassRequest request) {
        OrgDepartmentEntity dep = orgDepartmentMapper.selectById(request.departmentId());
        if (dep == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "院系不存在");
        }
        OrgClassEntity dup = orgClassMapper.selectOne(new LambdaQueryWrapper<OrgClassEntity>()
                .eq(OrgClassEntity::getDepartmentId, request.departmentId())
                .eq(OrgClassEntity::getName, request.name().trim())
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "班级已存在");
        }
        OrgClassEntity entity = new OrgClassEntity();
        entity.setDepartmentId(request.departmentId());
        entity.setName(request.name().trim());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        orgClassMapper.insert(entity);
        adminAuditService.record(actor, AdminAuditActions.CLASS_CREATE, "org_class", entity.getId(), Map.of(
                "departmentId", entity.getDepartmentId(),
                "name", entity.getName()
        ));
        return new AdminClassVO(entity.getId(), entity.getDepartmentId(), dep.getName(), entity.getName(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Transactional
    public AdminClassVO updateClass(AuthenticatedUser actor, Long id, AdminClassRequest request) {
        OrgClassEntity existing = orgClassMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "班级不存在");
        }
        OrgDepartmentEntity dep = orgDepartmentMapper.selectById(request.departmentId());
        if (dep == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "院系不存在");
        }
        OrgClassEntity dup = orgClassMapper.selectOne(new LambdaQueryWrapper<OrgClassEntity>()
                .eq(OrgClassEntity::getDepartmentId, request.departmentId())
                .eq(OrgClassEntity::getName, request.name().trim())
                .ne(OrgClassEntity::getId, id)
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "班级名称已被占用");
        }
        orgClassMapper.update(null, new LambdaUpdateWrapper<OrgClassEntity>()
                .eq(OrgClassEntity::getId, id)
                .set(OrgClassEntity::getDepartmentId, request.departmentId())
                .set(OrgClassEntity::getName, request.name().trim())
                .set(OrgClassEntity::getUpdatedAt, LocalDateTime.now()));
        adminAuditService.record(actor, AdminAuditActions.CLASS_UPDATE, "org_class", id, Map.of(
                "departmentId", request.departmentId(),
                "name", request.name().trim()
        ));
        OrgClassEntity updated = orgClassMapper.selectById(id);
        return new AdminClassVO(updated.getId(), updated.getDepartmentId(), dep.getName(), updated.getName(), updated.getCreatedAt(), updated.getUpdatedAt());
    }

    @Transactional
    public void deleteClass(AuthenticatedUser actor, Long id) {
        OrgClassEntity existing = orgClassMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "班级不存在");
        }
        long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getClassId, id));
        if (userCount > 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "仍有用户归属该班级，禁止删除");
        }
        orgClassMapper.deleteById(id);
        adminAuditService.record(actor, AdminAuditActions.CLASS_DELETE, "org_class", id, Map.of(
                "departmentId", existing.getDepartmentId(),
                "name", existing.getName()
        ));
    }

    public String exportDepartmentsCsv(AuthenticatedUser actor) {
        List<OrgDepartmentEntity> deps = orgDepartmentMapper.selectList(new LambdaQueryWrapper<OrgDepartmentEntity>().orderByAsc(OrgDepartmentEntity::getId));
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,createdAt\n");
        for (OrgDepartmentEntity d : deps) {
            csv.append(AdminCsv.cell(d.getId())).append(",");
            csv.append(AdminCsv.cell(d.getName())).append(",");
            csv.append(AdminCsv.cell(d.getCreatedAt())).append("\n");
        }
        adminAuditService.record(actor, AdminAuditActions.DEPARTMENT_EXPORT, "org_department", null, Map.of("count", deps.size()));
        return csv.toString();
    }

    public String exportClassesCsv(AuthenticatedUser actor) {
        List<AdminClassVO> classes = listClasses(null);
        StringBuilder csv = new StringBuilder();
        csv.append("id,departmentId,departmentName,name,createdAt\n");
        for (AdminClassVO c : classes) {
            csv.append(AdminCsv.cell(c.id())).append(",");
            csv.append(AdminCsv.cell(c.departmentId())).append(",");
            csv.append(AdminCsv.cell(c.departmentName())).append(",");
            csv.append(AdminCsv.cell(c.name())).append(",");
            csv.append(AdminCsv.cell(c.createdAt())).append("\n");
        }
        adminAuditService.record(actor, AdminAuditActions.CLASS_EXPORT, "org_class", null, Map.of("count", classes.size()));
        return csv.toString();
    }
}

