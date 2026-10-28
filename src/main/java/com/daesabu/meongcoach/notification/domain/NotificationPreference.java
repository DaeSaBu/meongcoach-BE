package com.daesabu.meongcoach.notification.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
		name = "notification_preferences",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notification_preferences_user_id_hour", columnNames = {"user_id", "hour"}),
		indexes = @Index(name = "idx_notification_preferences_hour_enabled", columnList = "hour, enabled")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(nullable = false)
	private int hour;

	@Column(nullable = false)
	private boolean enabled;

	public static NotificationPreference create(Long userId, int hour, boolean enabled) {
		NotificationHour.validate(hour);
		NotificationPreference preference = new NotificationPreference();

		preference.userId = requireNonNull(userId);
		preference.hour = hour;
		preference.enabled = enabled;

		return preference;
	}

	public static List<NotificationPreference> createDefaults(Long userId) {
		return NotificationHour.ALLOWED_HOURS.stream()
				.map(hour -> create(userId, hour, NotificationHour.isDefaultEnabled(hour)))
				.toList();
	}

	public void change(boolean enabled) {
		this.enabled = enabled;
	}
}
