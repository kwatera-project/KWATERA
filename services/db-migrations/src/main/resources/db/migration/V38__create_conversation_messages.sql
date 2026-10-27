CREATE TABLE conversation_messages
(
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    sender_user_id UUID NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id)
        REFERENCES conversations (id) ON DELETE RESTRICT,
    CONSTRAINT ck_messages_content_length CHECK (char_length(content) BETWEEN 1 AND 4000)
);

CREATE INDEX idx_messages_conversation_history
    ON conversation_messages (conversation_id, created_at DESC, id DESC);
