package com.daesabu.meongcoach.support.domain;

import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "SupportConversation")
@Table(name = "support_conversations", indexes = @Index(
		name = "support_conversations_activity", columnList = "updated_at DESC, id DESC"),
		uniqueConstraints = @UniqueConstraint(name = "uk_support_conversations_member", columnNames = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatConversation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private long memberId;

	@Column(nullable = false)
	private Instant updatedAt;

	public void verifyAccess(long userId, boolean operator) {
		if (memberId != userId && !operator) {
			throw new ChatAccessDeniedException();
		}
	}

	public void recordMessage() {
		updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}
}
