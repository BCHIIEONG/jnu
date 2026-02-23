package cn.edu.jnu.labflowreport.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserCreateRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserImportError;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserImportResult;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserUpdateRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserVO;
import cn.edu.jnu.labflowreport.admin.dto.PageResult;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgDepartmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysRoleEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserRoleEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgDepartmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysRoleMapper;
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
    private final OrgDepartmentMapper orgDepartmentMapper;
    private final OrgClassMapper orgClassMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditService adminAuditService;

    public AdminUserService(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            OrgDepartmentMapper orgDepartmentMapper,
            OrgClassMapper orgClassMapper,
            PasswordEncoder passwordEncoder,
            AdminAuditService adminAuditService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.orgDepartmentMapper = orgDepartmentMapper;
        this.orgClassMapper = orgClassMapper;
        this.passwordEncoder = passwordEncoder;
        this.adminAuditService = adminAuditService;
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
            wrapper.eq(SysUserEntity::getClassId, classId);
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
        entity.setClassId(request.classId());
        entity.setPasswordHash(passwordEncoder.encode(StringUtils.hasText(request.password()) ? request.password() : DEFAULT_PASSWORD));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(entity);

        setUserRolesInternal(entity.getId(), request.roleCodes());
        adminAuditService.record(actor, AdminAuditActions.USER_CREATE, "sys_user", entity.getId(), Map.of(
                "username", entity.getUsername(),
                "displayName", entity.getDisplayName(),
                "enabled", entity.getEnabled(),
                "departmentId", entity.getDepartmentId(),
                "classId", entity.getClassId(),
                "roleCodes", request.roleCodes()
        ));

        return getUser(entity.getId());
    }

    @Transactional
    public AdminUserVO updateUser(AuthenticatedUser actor, Long userId, AdminUserUpdateRequest request) {
        SysUserEntity existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "用户不存在");
        }

        LambdaUpdateWrapper<SysUserEntity> upd = new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId)
                .set(SysUserEntity::getUpdatedAt, LocalDateTime.now());

        Map<String, Object> changed = new HashMap<>();
        if (request.displayName() != null) {
            upd.set(SysUserEntity::getDisplayName, request.displayName());
            changed.put("displayName", request.displayName());
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
            upd.set(SysUserEntity::getClassId, request.classId());
            changed.put("classId", request.classId());
        }

        if (changed.isEmpty()) {
            return getUser(userId);
        }
        sysUserMapper.update(null, upd);
        adminAuditService.record(actor, AdminAuditActions.USER_UPDATE, "sys_user", userId, changed);
        return getUser(userId);
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
        setUserRolesInternal(userId, roleCodes);
        adminAuditService.record(actor, AdminAuditActions.USER_SET_ROLES, "sys_user", userId, Map.of(
                "username", existing.getUsername(),
                "roleCodes", roleCodes
        ));
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
                    Long classId = resolveClassId(departmentId, className);

                    SysUserEntity existing = sysUserMapper.findByUsername(username);
                    if (existing == null) {
                        AdminUserCreateRequest req = new AdminUserCreateRequest(
                                username,
                                displayName,
                                password,
                                enabled == null ? Boolean.TRUE : enabled,
                                departmentId,
                                classId,
                                roleCodes
                        );
                        createUser(actor, req);
                        created++;
                    } else {
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
                        if (classId != null) {
                            upd.set(SysUserEntity::getClassId, classId);
                        }
                        if (StringUtils.hasText(password)) {
                            upd.set(SysUserEntity::getPasswordHash, passwordEncoder.encode(password));
                        }
                        sysUserMapper.update(null, upd);
                        setUserRolesInternal(existing.getId(), roleCodes);
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
        csv.append("id,username,displayName,enabled,roleCodes,departmentId,departmentName,classId,className,createdAt\n");
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
            csv.append(AdminCsv.cell(u.createdAt())).append("\n");
        }

        adminAuditService.record(actor, AdminAuditActions.USER_EXPORT, "sys_user", null, Map.of("count", vos.size()));
        return csv.toString();
    }

    private void setUserRolesInternal(Long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "roleCodes 不能为空");
        }
        List<String> normalized = roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
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

        Map<Long, String> deptNames = deptIds.isEmpty() ? Map.of() : orgDepartmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(OrgDepartmentEntity::getId, OrgDepartmentEntity::getName));
        Map<Long, OrgClassEntity> classes = classIds.isEmpty() ? Map.of() : orgClassMapper.selectBatchIds(classIds).stream()
                .collect(Collectors.toMap(OrgClassEntity::getId, c -> c));

        List<AdminUserVO> result = new ArrayList<>(users.size());
        for (SysUserEntity u : users) {
            List<String> roles = sysUserMapper.findRoleCodesByUserId(u.getId());
            OrgClassEntity clazz = (u.getClassId() == null ? null : classes.get(u.getClassId()));
            result.add(new AdminUserVO(
                    u.getId(),
                    u.getUsername(),
                    u.getDisplayName(),
                    u.getEnabled(),
                    u.getDepartmentId(),
                    u.getDepartmentId() == null ? null : deptNames.get(u.getDepartmentId()),
                    u.getClassId(),
                    clazz == null ? null : clazz.getName(),
                    roles == null ? List.of() : roles,
                    u.getCreatedAt(),
                    u.getUpdatedAt()
            ));
        }
        return result;
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

    private Long resolveClassId(Long departmentId, String className) {
        if (departmentId == null || !StringUtils.hasText(className)) {
            return null;
        }
        OrgClassEntity clazz = orgClassMapper.selectOne(new LambdaQueryWrapper<OrgClassEntity>()
                .eq(OrgClassEntity::getDepartmentId, departmentId)
                .eq(OrgClassEntity::getName, className.trim())
                .last("LIMIT 1"));
        return clazz == null ? null : clazz.getId();
    }
}
