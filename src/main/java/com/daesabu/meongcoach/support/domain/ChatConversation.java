package com.daesabu.meongcoach.support.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

@Entity(name = "SupportConversation")
@Table(name = "support_conversations", indexes = {
		@Index(name = "support_conversations_activity", columnList = "last_message_at DESC, id DESC"),
		@Index(name = "uk_support_conversations_active_member", columnList = "(member_id)", unique = true, options = "WHERE deleted_at IS NULL")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatConversation extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private long memberId;

	private LocalDateTime deletedAt;

	@Column(nullable = false)
	@ColumnDefault("LOCALTIMESTAMP")
	private LocalDateTime lastMessageAt;

	public void recordMessage() {
		lastMessageAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
	}

	public boolean isOwnedBy(long userId) {
		return memberId == userId;
	}

	public boolean isClosed() {
		return deletedAt != null;
	}

	public void leave() {
		deletedAt = LocalDateTime.now();
	}
}
