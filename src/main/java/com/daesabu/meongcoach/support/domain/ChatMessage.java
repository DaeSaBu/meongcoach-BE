package com.daesabu.meongcoach.support.domain;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "SupportMessage")
@Convert(attributeName = "createdAt", converter = ChatTimestampConverter.class)
@Table(name = "support_messages",
		indexes = @Index(name = "support_messages_conversation_cursor", columnList = "conversation_id, id"),
		uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "sender_id", "client_message_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {
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

	private ChatMessage(ChatMessageCommand command) {
		this.conversationId = command.conversationId();
		this.senderId = command.senderId();
		this.operator = command.operator();
		this.clientMessageId = command.clientMessageId();
		this.body = command.body();
	}

	public static ChatMessage create(ChatMessageCommand command) {
		return new ChatMessage(command);
	}

	public ChatMessageSentEvent sentEvent(long memberId) {
		return new ChatMessageSentEvent(id, memberId, senderId);
	}
}
