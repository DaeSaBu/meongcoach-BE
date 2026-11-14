package com.daesabu.meongcoach.support.adapter.persistence;

import com.daesabu.meongcoach.support.domain.ChatMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity(name = "SupportMessage")
@Table(name = "support_messages", uniqueConstraints = @UniqueConstraint(
		columnNames = {"conversation_id", "sender_id", "client_message_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ChatMessageEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private long conversationId;

	@Column(nullable = false)
	private long senderId;

	@Column(nullable = false)
	private boolean operator;

	@Column(nullable = false)
	private UUID clientMessageId;

	@Column(nullable = false, length = 2000)
	private String body;

	@Column(nullable = false)
	private Instant createdAt;

	ChatMessageEntity(long conversationId, long senderId, boolean operator, UUID clientMessageId, String body) {
		this.conversationId = conversationId;
		this.senderId = senderId;
		this.operator = operator;
		this.clientMessageId = clientMessageId;
		this.body = body;
		this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	ChatMessage toDomain() {
		return new ChatMessage(id, conversationId, senderId, operator, clientMessageId, body, createdAt);
	}
}
