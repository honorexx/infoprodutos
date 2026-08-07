CREATE TABLE audit_log (
    id             UUID PRIMARY KEY,
    actor_user_id  UUID REFERENCES app_user (id) ON DELETE SET NULL,
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      UUID NOT NULL,
    metadata       JSONB,
    ip_address     VARCHAR(45),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);

COMMENT ON TABLE audit_log IS 'Nunca excluído fisicamente (docs/DATABASE.md secao 3).';
