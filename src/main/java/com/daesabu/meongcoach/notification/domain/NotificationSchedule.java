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
		name = "notification_schedules",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_notification_schedules_user_id_hour_minute", columnNames = {"user_id", "hour", "minute"}),
		indexes = @Index(name = "idx_notification_schedules_hour_minute_enabled", columnList = "hour, minute, enabled")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSchedule extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(nullable = false)
	private int hour;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(nullable = false)
	private int minute;

	@Column(nullable = false)
	private boolean enabled;

	public static NotificationSchedule create(Long userId, NotificationTime time, boolean enabled) {
		NotificationSchedule schedule = new NotificationSchedule();

		schedule.userId = requireNonNull(userId);
		schedule.hour = time.hour();
		schedule.minute = time.minute();
		schedule.enabled = enabled;

		return schedule;
	}

	public static List<NotificationSchedule> createDefaults(Long userId) {
		return NotificationTime.ALLOWED.stream()
				.map(time -> create(userId, time, time.isDefaultEnabled()))
				.toList();
	}

	public NotificationTime time() {
		return new NotificationTime(hour, minute);
	}

	public void change(boolean enabled) {
		this.enabled = enabled;
	}
}
