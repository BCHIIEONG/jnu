package cn.edu.jnu.labflowreport.admin.service;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.persistence.entity.AuditLogEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.AuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AdminAuditService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void record(AuthenticatedUser actor, String action, String targetType, Long targetId, Map<String, Object> detail) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setActorId(actor.userId());
        entity.setActorUsername(actor.username());
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setDetailJson(toJson(detail));
        entity.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(entity);
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"detail_json_serialize_failed\"}";
        }
    }
}

