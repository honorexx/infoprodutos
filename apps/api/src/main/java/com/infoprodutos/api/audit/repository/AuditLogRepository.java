package com.infoprodutos.api.audit.repository;

import com.infoprodutos.api.audit.domain.AuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {}
