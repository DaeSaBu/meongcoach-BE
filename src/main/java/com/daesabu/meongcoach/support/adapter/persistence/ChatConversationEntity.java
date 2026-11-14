package com.daesabu.meongcoach.support.adapter.persistence;

import com.daesabu.meongcoach.support.domain.ChatConversation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity(name = "SupportConversation")
@Table(name = "support_conversations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ChatConversationEntity {
	@Id
	private Long id;

	@Column(nullable = false)
	private Instant updatedAt;

	void recordMessage() {
		updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	ChatConversation toDomain() {
		return new ChatConversation(id, updatedAt);
	}
}
