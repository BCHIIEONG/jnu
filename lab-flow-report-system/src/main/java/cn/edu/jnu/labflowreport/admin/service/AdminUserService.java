package cn.edu.jnu.labflowreport.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserCreateRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserImportError;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserImportResult;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserUpdateRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserVO;
import cn.edu.jnu.labflowreport.admin.dto.PageResult;
import cn.edu.jnu.labflowreport.attendance.mapper.AttendanceRecordMapper;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.common.util.ClassDisplayUtils;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgDepartmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysRoleEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserRoleEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgDepartmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportSubmissionMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysRoleMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserRoleMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminUserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserClassMapper sysUserClassMapper;
    private final OrgDepartmentMapper orgDepartmentMapper;
    private final OrgClassMapper orgClassMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditService adminAuditService;
    private final ReportSubmissionMapper reportSubmissionMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;

    public AdminUserService(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysUserClassMapper sysUserClassMapper,
            OrgDepartmentMapper orgDepartmentMapper,
            OrgClassMapper orgClassMapper,
            PasswordEncoder passwordEncoder,
            AdminAuditService adminAuditService,
            ReportSubmissionMapper reportSubmissionMapper,
            AttendanceRecordMapper attendanceRecordMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysUserClassMapper = sysUserClassMapper;
        this.orgDepartmentMapper = orgDepartmentMapper;
        this.orgClassMapper = orgClassMapper;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditService = adminAuditService;
        this.reportSubmissionMapper = reportSubmissionMapper;
        this.attendanceRecordMapper = attendanceRecordMapper;
    }

    public PageResult<AdminUserVO> listUsers(
            String q,
            String roleCode,
            Boolean enabled,
            Long departmentId,
            Long classId,
            int page,
            int size
    ) {
        page = Math.max(1, page);
        size = Math.min(Math.max(1, size), 200);

        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q)) {
            wrapper.and(w -> w.like(SysUserEntity::getUsername, q).or().like(SysUserEntity::getDisplayName, q));
        }
        if (enabled != null) {
            wrapper.eq(SysUserEntity::getEnabled, enabled);
        }
        if (departmentId != null) {
            wrapper.eq(SysUserEntity::getDepartmentId, departmentId);
        }
        if (classId != null) {
            List<Long> teacherIds = sysUserClassMapper.findUserIdsByClassId(classId);
            if (teacherIds == null || teacherIds.isEmpty()) {
                wrapper.eq(SysUserEntity::getClassId, classId);
            } else {
                wrapper.and(w -> w.eq(SysUserEntity::getClassId, classId).or().in(SysUserEntity::getId, teacherIds));
            }
        }

        if (StringUtils.hasText(roleCode)) {
            SysRoleEntity role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                    .eq(SysRoleEntity::getCode, roleCode.trim())
                    .last("LIMIT 1"));
            if (role == null) {
                return new PageResult<>(page, size, 0, List.of());
            }
            List<Long> userIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                            .eq(SysUserRoleEntity::getRoleId, role.getId()))
                    .stream()
                    .map(SysUserRoleEntity::getUserId)
                    .distinct()
                    .toList();
            if (userIds.isEmpty()) {
                return new PageResult<>(page, size, 0, List.of());
            }
            wrapper.in(SysUserEntity::getId, userIds);
        }

        long total = sysUserMapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResult<>(page, size, 0, List.of());
        }

        int offset = (page - 1) * size;
        List<SysUserEntity> users = sysUserMapper.selectList(wrapper
                .orderByDesc(SysUserEntity::getId)
                .last("LIMIT " + size + " OFFSET " + offset));

        return new PageResult<>(page, size, total, toUserVOs(users));
    }

    public AdminUserVO getUser(Long userId) {
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "用户不存在");
        }
        return toUserVOs(List.of(user)).get(0);
    }

    @Transactional
    public AdminUserVO createUser(AuthenticatedUser actor, AdminUserCreateRequest request) {
        String username = safeTrim(request.username());
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, "username 不能为空");
        }
        if (sysUserMapper.findByUsername(username) != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "用户名已存在");
        }

        SysUserEntity entity = new SysUserEntity();
        entity.setUsername(username);
        entity.setDisplayName(safeTrim(request.displayName()));
        entity.setEnabled(request.enabled() == null ? Boolean.TRUE : request.enabled());
        entity.setDepartmentId(request.departmentId());
        List<String> normalizedRoleCodes = normalizeRoleCodes(request.roleCodes());
        validateRoleCombination(normalizedRoleCodes);
        entity.setClassId(resolveSingleClassId(normalizedRoleCodes, request.classId()));
        entity.setPasswordHash(passwordEncoder.encode(StringUtils.hasText(request.password()) ? request.password() : DEFAULT_PASSWORD));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(entity);

        setUserRolesInternal(entity.getId(), normalizedRoleCodes);
        replaceTeacherClassBindings(entity.getId(), normalizedRoleCodes, normalizeClassIds(request.classIds(), request.classId()));
        Map<String, Object> detail = new HashMap<>();
        detail.put("username", entity.getUsername());
        detail.put("displayName", entity.getDisplayName());
        detail.put("enabled", entity.getEnabled());
        detail.put("departmentId", entity.getDepartmentId());
        detail.put("classId", entity.getClassId());
        detail.put("classIds", normalizeClassIds(request.classIds(), request.classId()));
        detail.put("roleCodes", normalizedRoleCodes);
        adminAuditService.record(actor, AdminAuditActions.USER_CREATE, "sys_user", entity.getId(), detail);

        return getUser(entity.getId());
    }

    @Transactional
    public AdminUserVO updateUser(AuthenticatedUser actor, Long userId, AdminUserUpdateRequest request) {
        SysUserEntity existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "用户不存在");
        }
        List<String> roleCodes = normalizeRoleCodes(sysUserMapper.findRoleCodesByUserId(userId));
        validateRoleCombination(roleCodes);
        List<Long> normalizedClassIds = normalizeClassIds(request.classIds(), request.classId());
        boolean teacherUser = roleCodes.contains("ROLE_TEACHER");

        LambdaUpdateWrapper<SysUserEntity> upd = new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId)
                .set(SysUserEntity::getUpdatedAt, LocalDateTime.now());

        Map<String, Object> changed = new HashMap<>();
        if (request.username() != null) {
            String username = request.username().trim();
            if (username.isEmpty()) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "username 不能为空");
            }
            SysUserEntity sameName = sysUserMapper.findByUsername(username);
            if (sameName != null && !sameName.getId().equals(userId)) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "username 已存在");
            }
            upd.set(SysUserEntity::getUsername, username);
            changed.put("username", username);
        }
        if (request.displayName() != null) {
            String displayName = request.displayName().trim();
            if (displayName.isEmpty()) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "displayName 不能为空");
            }
            upd.set(SysUserEntity::getDisplayName, displayName);
            changed.put("displayName", displayName);
        }
        if (request.enabled() != null) {
            upd.set(SysUserEntity::getEnabled, request.enabled());
            changed.put("enabled", request.enabled());
        }
        if (request.departmentId() != null) {
            upd.set(SysUserEntity::getDepartmentId, request.departmentId());
            changed.put("departmentId", request.departmentId());
        }
        if (request.classId() != null) {
            if (teacherUser) {
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "教师账号请使用多班级绑定");
            }
            upd.set(SysUserEntity::getClassId, request.classId());
            changed.put("classId", request.classId());
        }
        if (request.classIds() != null) {
            if (!teacherUser) {
                if (shouldIgnoreNonTeacherClassIds(request.classId(), normalizedClassIds)) {
                    return applyUserUpdate(actor, userId, upd, changed);
                }
                throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "仅教师账号支持多班级绑定");
            }
            upd.set(SysUserEntity::getClassId, null);
            replaceTeacherClassBindings(userId, roleCodes, normalizedClassIds);
            changed.put("classIds", normalizedClassIds);
        }

        return applyUserUpdate(actor, userId, upd, changed);
    }

    @Transactional
    public void resetPassword(AuthenticatedUser actor, Long userId, String newPassword) {
        SysUserEntity existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "用户不存在");
        }
        String pw = StringUtils.hasText(newPassword) ? newPassword : DEFAULT_PASSWORD;
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId)
                .set(SysUserEntity::getPasswordHash, passwordEncoder.encode(pw))
                .set(SysUserEntity::getUpdatedAt, LocalDateTime.now()));
        adminAuditService.record(actor, AdminAuditActions.USER_RESET_PASSWORD, "sys_user", userId, Map.of("username", existing.getUsername()));
    }

    @Transactional
    public void setUserRoles(AuthenticatedUser actor, Long userId, List<String> roleCodes) {
        SysUserEntity existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "用户不存在");
        }
        List<String> normalizedRoleCodes = normalizeRoleCodes(roleCodes);
        validateRoleCombination(normalizedRoleCodes);
        setUserRolesInternal(userId, normalizedRoleCodes);
        adminAuditService.record(actor, AdminAuditActions.USER_SET_ROLES, "sys_user", userId, Map.of(
                "username", existing.getUsername(),
                "roleCodes", normalizedRoleCodes
        ));
    }

    @Transactional
    public void deleteStudentUser(AuthenticatedUser actor, Long userId) {
        SysUserEntity existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "用户不存在");
        }

        List<String> roles = sysUserMapper.findRoleCodesByUserId(userId);
        boolean hasStudent = roles != null && roles.contains("ROLE_STUDENT");
        boolean hasTeacherOrAdmin = roles != null && (roles.contains("ROLE_TEACHER") || roles.contains("ROLE_ADMIN"));
        if (!hasStudent || hasTeacherOrAdmin) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "只能删除“仅学生”账号（不能删除教师/管理员或混合角色账号）");
        }

        long submissionCount = reportSubmissionMapper.countByStudentId(userId);
        long attendanceCount = attendanceRecordMapper.countByStudentId(userId);
        if (submissionCount > 0 || attendanceCount > 0) {
            throw new BusinessException(ApiCode.BAD_REQUEST,
                    "该学生已有业务数据（报告提交/签到记录），不允许删除；请改为禁用账号");
        }

        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));
        sysUserMapper.deleteById(userId);
        adminAuditService.record(actor, AdminAuditActions.USER_DELETE, "sys_user", userId, Map.of("username", existing.getUsername()));
    }

    @Transactional
    public AdminUserImportResult importUsers(AuthenticatedUser actor, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "csv 文件为空");
        }

        List<AdminUserImportError> errors = new ArrayList<>();
        int total = 0;
        int created = 0;
        int updated = 0;

        try (InputStream in = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException(ApiCode.BAD_REQUEST, "csv 文件为空");
            }
            List<String> headers = normalizeHeaders(parseCsvLine(stripBom(headerLine)));
            Map<String, Integer> idx = indexOf(headers);
            requireHeader(idx, "username");
            requireHeader(idx, "displayName");

            String line;
            int rowNo = 1;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                total++;
                try {
                    List<String> cols = parseCsvLine(line);
                    String username = safeTrim(get(cols, idx.get("username")));
                    String displayName = safeTrim(get(cols, idx.get("displayName")));
                    String password = idx.containsKey("password") ? get(cols, idx.get("password")) : null;
                    String enabledText = idx.containsKey("enabled") ? get(cols, idx.get("enabled")) : null;
                    String roleCodesText = idx.containsKey("roleCodes") ? get(cols, idx.get("roleCodes")) : null;
                    String departmentName = idx.containsKey("departmentName") ? get(cols, idx.get("departmentName")) : null;
                    String className = idx.containsKey("className") ? get(cols, idx.get("className")) : null;

                    if (!StringUtils.hasText(username) || !StringUtils.hasText(displayName)) {
                        throw new BusinessException(ApiCode.VALIDATION_ERROR, "username/displayName 不能为空");
                    }

                    Boolean enabled = parseBool(enabledText);
                    List<String> roleCodes = parseRoleCodes(roleCodesText);
                    if (roleCodes.isEmpty()) {
                        roleCodes = List.of("ROLE_STUDENT");
                    }

                    Long departmentId = resolveDepartmentId(departmentName);
                    List<Long> classIds = resolveClassIds(departmentId, className);
                    Long classId = classIds.isEmpty() ? null : classIds.get(0);

                    SysUserEntity existing = sysUserMapper.findByUsername(username);
                    if (existing == null) {
                        AdminUserCreateRequest req = new AdminUserCreateRequest(
                                username,
                                displayName,
                                password,
                                enabled == null ? Boolean.TRUE : enabled,
                                departmentId,
                                classId,
                                classIds,
                                roleCodes
                        );
                        createUser(actor, req);
                        created++;
                    } else {
                        List<String> normalizedRoleCodes = normalizeRoleCodes(roleCodes);
                        validateRoleCombination(normalizedRoleCodes);
                        LambdaUpdateWrapper<SysUserEntity> upd = new LambdaUpdateWrapper<SysUserEntity>()
                                .eq(SysUserEntity::getId, existing.getId())
                                .set(SysUserEntity::getDisplayName, displayName)
                                .set(SysUserEntity::getUpdatedAt, LocalDateTime.now());
                        if (enabled != null) {
                            upd.set(SysUserEntity::getEnabled, enabled);
                        }
                        if (departmentId != null) {
                            upd.set(SysUserEntity::getDepartmentId, departmentId);
                        }
                        if (normalizedRoleCodes.contains("ROLE_TEACHER")) {
                            upd.set(SysUserEntity::getClassId, null);
                        } else if (classId != null) {
                            upd.set(SysUserEntity::getClassId, classId);
                        }
                        if (StringUtils.hasText(password)) {
                            upd.set(SysUserEntity::getPasswordHash, passwordEncoder.encode(password));
                        }
                        sysUserMapper.update(null, upd);
                        setUserRolesInternal(existing.getId(), normalizedRoleCodes);
                        replaceTeacherClassBindings(existing.getId(), normalizedRoleCodes, classIds);
                        adminAuditService.record(actor, AdminAuditActions.USER_UPDATE, "sys_user", existing.getId(), Map.of("import", true, "username", username));
                        updated++;
                    }
                } catch (Exception ex) {
                    String msg = ex instanceof BusinessException ? ex.getMessage() : "解析或写入失败";
                    errors.add(new AdminUserImportError(rowNo, null, msg));
                }
            }
        } catch (IOException ex) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "读取 csv 文件失败");
        }

        adminAuditService.record(actor, AdminAuditActions.USER_IMPORT, "sys_user", null, Map.of(
                "totalRows", total,
                "created", created,
                "updated", updated,
                "failed", errors.size()
        ));

        return new AdminUserImportResult(total, created, updated, errors.size(), errors);
    }

    public String exportUsersCsv(AuthenticatedUser actor) {
        List<SysUserEntity> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>().orderByAsc(SysUserEntity::getId));
        List<AdminUserVO> vos = toUserVOs(users);

        StringBuilder csv = new StringBuilder();
        csv.append("id,username,displayName,enabled,roleCodes,departmentId,departmentName,classId,className,classIds,classDisplayText,createdAt\n");
        for (AdminUserVO u : vos) {
            csv.append(AdminCsv.cell(u.id())).append(",");
            csv.append(AdminCsv.cell(u.username())).append(",");
            csv.append(AdminCsv.cell(u.displayName())).append(",");
            csv.append(AdminCsv.cell(u.enabled())).append(",");
            csv.append(AdminCsv.cell(String.join("|", u.roleCodes()))).append(",");
            csv.append(AdminCsv.cell(u.departmentId())).append(",");
            csv.append(AdminCsv.cell(u.departmentName())).append(",");
            csv.append(AdminCsv.cell(u.classId())).append(",");
            csv.append(AdminCsv.cell(u.className())).append(",");
            csv.append(AdminCsv.cell(u.classIds() == null ? null : u.classIds().stream().map(String::valueOf).collect(Collectors.joining("|")))).append(",");
            csv.append(AdminCsv.cell(u.classDisplayText())).append(",");
            csv.append(AdminCsv.cell(u.createdAt())).append("\n");
        }

        adminAuditService.record(actor, AdminAuditActions.USER_EXPORT, "sys_user", null, Map.of("count", vos.size()));
        return csv.toString();
    }

    private void setUserRolesInternal(Long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "roleCodes 不能为空");
        }
        List<String> normalized = normalizeRoleCodes(roleCodes);
        List<SysRoleEntity> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>().in(SysRoleEntity::getCode, normalized));
        if (roles.size() != normalized.size()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "roleCodes 包含未知角色");
        }
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));
        for (SysRoleEntity role : roles) {
            SysUserRoleEntity ur = new SysUserRoleEntity();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            ur.setCreatedAt(LocalDateTime.now());
            sysUserRoleMapper.insert(ur);
        }
    }

    private List<AdminUserVO> toUserVOs(List<SysUserEntity> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        Set<Long> deptIds = users.stream().map(SysUserEntity::getDepartmentId).filter(id -> id != null && id > 0).collect(Collectors.toSet());
        Set<Long> classIds = users.stream().map(SysUserEntity::getClassId).filter(id -> id != null && id > 0).collect(Collectors.toSet());
        Map<Long, List<Long>> teacherClassIds = new HashMap<>();
        for (SysUserEntity user : users) {
            List<String> roleCodes = sysUserMapper.findRoleCodesByUserId(user.getId());
            if (roleCodes != null && roleCodes.contains("ROLE_TEACHER")) {
                List<Long> boundClassIds = sysUserClassMapper.findClassIdsByUserId(user.getId());
                teacherClassIds.put(user.getId(), boundClassIds == null ? List.of() : boundClassIds);
                classIds.addAll(teacherClassIds.get(user.getId()));
            }
        }

        Map<Long, String> deptNames = deptIds.isEmpty() ? Map.of() : orgDepartmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(OrgDepartmentEntity::getId, OrgDepartmentEntity::getName));
        Map<Long, OrgClassEntity> classes = classIds.isEmpty() ? Map.of() : orgClassMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(OrgClassEntity::getId, c -> c));

        List<AdminUserVO> result = new ArrayList<>(users.size());
        for (SysUserEntity u : users) {
            List<String> roles = sysUserMapper.findRoleCodesByUserId(u.getId());
            List<Long> boundClassIds = roles != null && roles.contains("ROLE_TEACHER")
                    ? teacherClassIds.getOrDefault(u.getId(), List.of())
                    : (u.getClassId() == null ? List.of() : List.of(u.getClassId()));
            List<String> boundClassNames = boundClassIds.stream()
                    .map(classes::get)
                    .filter(Objects::nonNull)
                    .map(c -> ClassDisplayUtils.effectiveDisplayName(c.getGrade(), c.getName()))
                    .toList();
            Long primaryClassId = roles != null && roles.contains("ROLE_TEACHER")
                    ? (boundClassIds.isEmpty() ? null : boundClassIds.get(0))
                    : u.getClassId();
            String primaryClassName = boundClassNames.isEmpty() ? null : boundClassNames.get(0);
            result.add(new AdminUserVO(
                    u.getId(),
                    u.getUsername(),
                    u.getDisplayName(),
                    u.getEnabled(),
                    u.getDepartmentId(),
                    u.getDepartmentId() == null ? null : deptNames.get(u.getDepartmentId()),
                    primaryClassId,
                    primaryClassName,
                    boundClassIds,
                    boundClassNames,
                    String.join(" / ", boundClassNames),
                    roles == null ? List.of() : roles,
                    u.getCreatedAt(),
                    u.getUpdatedAt()
                ));
        }
        return result;
    }

    private void replaceTeacherClassBindings(Long userId, List<String> roleCodes, List<Long> classIds) {
        sysUserClassMapper.delete(new LambdaQueryWrapper<SysUserClassEntity>().eq(SysUserClassEntity::getUserId, userId));
        if (!roleCodes.contains("ROLE_TEACHER")) {
            return;
        }
        for (Long classId : classIds) {
            SysUserClassEntity binding = new SysUserClassEntity();
            binding.setUserId(userId);
            binding.setClassId(classId);
            binding.setCreatedAt(LocalDateTime.now());
            sysUserClassMapper.insert(binding);
        }
    }

    private static List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null) {
            return List.of();
        }
        return roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static void validateRoleCombination(List<String> roleCodes) {
        if (roleCodes.contains("ROLE_TEACHER") && roleCodes.contains("ROLE_STUDENT")) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "暂不支持同时拥有教师和学生角色");
        }
    }

    private static List<Long> normalizeClassIds(List<Long> classIds, Long classId) {
        if (classIds != null && !classIds.isEmpty()) {
            return classIds.stream().filter(Objects::nonNull).distinct().toList();
        }
        if (classId != null) {
            return List.of(classId);
        }
        return List.of();
    }

    private static Long resolveSingleClassId(List<String> roleCodes, Long classId) {
        return roleCodes.contains("ROLE_TEACHER") ? null : classId;
    }

    private static boolean shouldIgnoreNonTeacherClassIds(Long classId, List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return true;
        }
        return classId != null && classIds.size() == 1 && Objects.equals(classIds.get(0), classId);
    }

    private AdminUserVO applyUserUpdate(
            AuthenticatedUser actor,
            Long userId,
            LambdaUpdateWrapper<SysUserEntity> upd,
            Map<String, Object> changed
    ) {
        if (changed.isEmpty()) {
            return getUser(userId);
        }
        sysUserMapper.update(null, upd);
        adminAuditService.record(actor, AdminAuditActions.USER_UPDATE, "sys_user", userId, changed);
        return getUser(userId);
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static List<String> normalizeHeaders(List<String> headers) {
        return headers.stream()
                .map(h -> h == null ? "" : h.trim())
                .map(h -> h.replace("\uFEFF", ""))
                .toList();
    }

    private static Map<String, Integer> indexOf(List<String> headers) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            idx.put(headers.get(i), i);
        }
        return idx;
    }

    private static void requireHeader(Map<String, Integer> idx, String name) {
        if (!idx.containsKey(name)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "csv 缺少列: " + name);
        }
    }

    private static String get(List<String> cols, Integer i) {
        if (i == null || i < 0 || i >= cols.size()) {
            return null;
        }
        String v = cols.get(i);
        return StringUtils.hasText(v) ? v.trim() : null;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else {
                if (ch == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else if (ch == '"') {
                    inQuotes = true;
                } else {
                    cur.append(ch);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static Boolean parseBool(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.equals("1") || t.equals("true") || t.equals("yes") || t.equals("y")) {
            return true;
        }
        if (t.equals("0") || t.equals("false") || t.equals("no") || t.equals("n")) {
            return false;
        }
        return null;
    }

    private static List<String> parseRoleCodes(String s) {
        if (!StringUtils.hasText(s)) {
            return List.of();
        }
        String normalized = s.trim().replace(",", "|");
        return List.of(normalized.split("\\|")).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private Long resolveDepartmentId(String departmentName) {
        if (!StringUtils.hasText(departmentName)) {
            return null;
        }
        OrgDepartmentEntity dep = orgDepartmentMapper.selectOne(new LambdaQueryWrapper<OrgDepartmentEntity>()
                .eq(OrgDepartmentEntity::getName, departmentName.trim())
                .last("LIMIT 1"));
        return dep == null ? null : dep.getId();
    }

    private List<Long> resolveClassIds(Long departmentId, String classNamesText) {
        if (departmentId == null || !StringUtils.hasText(classNamesText)) {
            return List.of();
        }
        List<OrgClassEntity> classes = orgClassMapper.selectList(new LambdaQueryWrapper<OrgClassEntity>()
                .eq(OrgClassEntity::getDepartmentId, departmentId));
        List<String> wantedNames = List.of(classNamesText.trim().replace("；", "|").replace(",", "|").split("\\|")).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        List<Long> resolved = new ArrayList<>();
        for (String wanted : wantedNames) {
            for (OrgClassEntity clazz : classes) {
                if (wanted.equals(clazz.getName()) || wanted.equals(ClassDisplayUtils.effectiveDisplayName(clazz.getGrade(), clazz.getName()))) {
                    resolved.add(clazz.getId());
                    break;
                }
            }
        }
        return resolved.stream().distinct().toList();
    }
}
