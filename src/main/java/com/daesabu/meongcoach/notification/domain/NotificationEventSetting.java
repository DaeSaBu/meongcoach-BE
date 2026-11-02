package com.daesabu.meongcoach.notification.domain;

import static java.util.Objects.requireNonNull;

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
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "notification_event_settings",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notification_event_settings_user_id_type", columnNames = {"user_id", "type"}),
		indexes = @Index(name = "idx_notification_event_settings_type_enabled_user_id", columnList = "type, enabled, user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEventSetting extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationEventType type;

	@Column(nullable = false)
	private boolean enabled;

	public static NotificationEventSetting create(Long userId, NotificationEventType type, boolean enabled) {
		NotificationEventSetting setting = new NotificationEventSetting();

		setting.userId = requireNonNull(userId);
		setting.type = requireNonNull(type);
		setting.enabled = enabled;

		return setting;
	}

	public static List<NotificationEventSetting> createDefaults(Long userId) {
		return Arrays.stream(NotificationEventType.values())
				.map(type -> create(userId, type, true))
				.toList();
	}

	public void change(boolean enabled) {
		this.enabled = enabled;
	}
}
