package com.daesabu.meongcoach.notification.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "notification_preferences",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notification_preferences_user_id_slot", columnNames = {"user_id", "slot"}),
		indexes = @Index(name = "idx_notification_preferences_slot_enabled", columnList = "slot, enabled")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private NotificationSlot slot;

	@Column(nullable = false)
	private boolean enabled;

	static NotificationPreference create(Long userId, NotificationSlot slot, boolean enabled) {
		NotificationPreference preference = new NotificationPreference();

		preference.userId = requireNonNull(userId);
		preference.slot = requireNonNull(slot);
		preference.enabled = enabled;

		return preference;
	}

	void change(boolean enabled) {
		this.enabled = enabled;
	}

	SlotPreference toSlotPreference() {
		return new SlotPreference(slot, enabled);
	}
}
