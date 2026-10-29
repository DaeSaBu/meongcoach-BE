package com.daesabu.meongcoach.notification.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "device_tokens",
		uniqueConstraints = @UniqueConstraint(name = "uk_device_tokens_token", columnNames = "token"),
		indexes = @Index(name = "idx_device_tokens_user_id", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 512)
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DevicePlatform platform;

	@Column(nullable = false)
	private Instant lastSeenAt;

	public static DeviceToken register(Long userId, DeviceTokenRegisterCommand command, Instant lastSeenAt) {
		DeviceToken deviceToken = new DeviceToken();

		deviceToken.userId = requireNonNull(userId);
		deviceToken.token = requireNonNull(command.token());
		deviceToken.platform = requireNonNull(command.platform());
		deviceToken.lastSeenAt = requireNonNull(lastSeenAt);

		return deviceToken;
	}

	public void renew(Long userId, Instant lastSeenAt) {
		this.userId = requireNonNull(userId);
		this.lastSeenAt = requireNonNull(lastSeenAt);
	}
}
