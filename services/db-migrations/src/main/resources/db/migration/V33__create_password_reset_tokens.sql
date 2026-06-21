CREATE TABLE password_reset_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens (token);
