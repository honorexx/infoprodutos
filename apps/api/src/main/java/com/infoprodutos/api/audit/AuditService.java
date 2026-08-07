package com.infoprodutos.api.audit;

import com.infoprodutos.api.audit.domain.AuditLog;
import com.infoprodutos.api.audit.repository.AuditLogRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Registro de auditoria para ações administrativas relevantes
 * (docs/SECURITY.md secao 6). AuditLog nunca é excluído.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(UUID actorUserId, String action, String entityType, UUID entityId, String metadataJson) {
        AuditLog log = new AuditLog(actorUserId, action, entityType, entityId, metadataJson, null);
        auditLogRepository.save(log);
    }
}
