-- Notificações in-app por usuário (header da área autenticada).
CREATE TABLE notification (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    type            VARCHAR(60) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            VARCHAR(500) NOT NULL,
    link_href       VARCHAR(500),
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_user_unread ON notification (user_id) WHERE read_at IS NULL;

COMMENT ON TABLE notification IS 'Notificações pessoais (boas-vindas, matrícula, acesso ao curso).';
