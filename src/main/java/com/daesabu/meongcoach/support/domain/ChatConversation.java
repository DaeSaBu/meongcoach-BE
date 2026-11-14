package com.daesabu.meongcoach.support.domain;

import com.daesabu.meongcoach.support.domain.exception.ChatAccessDeniedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity(name = "SupportConversation")
@Table(name = "support_conversations", indexes = {
		@Index(name = "support_conversations_activity", columnList = "updated_at DESC, id DESC"),
		@Index(name = "uk_support_conversations_active_member", columnList = "(member_id)", unique = true, options = "WHERE deleted_at IS NULL")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatConversation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private long memberId;

	private Instant deletedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@PrePersist
	void initTimestamps() {
		createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		updatedAt = createdAt;
	}

	public void verifyAccess(long userId, boolean operator) {
		if (deletedAt != null || (memberId != userId && !operator)) {
			throw new ChatAccessDeniedException();
		}
	}

	@PreUpdate
	public void recordMessage() {
		updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	public void leave() {
		deletedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}
}
