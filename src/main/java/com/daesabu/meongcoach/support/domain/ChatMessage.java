package com.daesabu.meongcoach.support.domain;

import com.daesabu.meongcoach.support.domain.exception.InvalidChatMessageException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "SupportMessage")
@Table(name = "support_messages",
		indexes = @Index(name = "support_messages_conversation_cursor", columnList = "conversation_id, id"),
		uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "sender_id", "client_message_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {
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

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void initCreatedAt() {
		createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	@Embedded
	private ChatAttachment attachment;

	private ChatMessage(ChatMessageCommand command) {
		this.conversationId = command.conversationId();
		this.senderId = command.senderId();
		this.operator = command.operator();
		this.clientMessageId = command.clientMessageId();
		this.body = Objects.requireNonNullElse(command.body(), "").strip();
		this.attachment = command.attachment();
		if ((body.isEmpty() && attachment == null) || body.length() > 2000) {
			throw new InvalidChatMessageException();
		}
	}

	public static ChatMessage create(ChatMessageCommand command) {
		return new ChatMessage(command);
	}

	public ChatMessageSentEvent sentEvent(long memberId) {
		return new ChatMessageSentEvent(id, memberId, senderId);
	}
}
