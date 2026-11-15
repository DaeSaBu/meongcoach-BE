CREATE TABLE support_conversations (
 id BIGINT PRIMARY KEY REFERENCES users(id),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX support_conversations_activity ON support_conversations(updated_at DESC,id DESC);
CREATE TABLE support_messages (
 id BIGSERIAL PRIMARY KEY,
 conversation_id BIGINT NOT NULL REFERENCES support_conversations(id),
 sender_id BIGINT NOT NULL REFERENCES users(id),
 operator BOOLEAN NOT NULL,
 client_message_id UUID NOT NULL,
 body VARCHAR(2000) NOT NULL CHECK (length(trim(body)) > 0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(conversation_id, sender_id, client_message_id)
);
CREATE INDEX support_messages_conversation_cursor ON support_messages(conversation_id,id);
CREATE TABLE support_socket_tickets (
 ticket UUID PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES users(id),
 operator BOOLEAN NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX support_socket_tickets_expiry ON support_socket_tickets(expires_at);
CREATE INDEX support_socket_tickets_user ON support_socket_tickets(user_id);
