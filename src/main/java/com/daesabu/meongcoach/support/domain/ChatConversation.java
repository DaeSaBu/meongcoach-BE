package com.daesabu.meongcoach.support.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "SupportConversation")
@Table(name = "support_conversations", indexes = @Index(
		name = "support_conversations_activity", columnList = "updated_at DESC, id DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatConversation {
	@Id
	private Long id;

	@Column(nullable = false)
	private Instant updatedAt;

	public void recordMessage() {
		updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}
}
