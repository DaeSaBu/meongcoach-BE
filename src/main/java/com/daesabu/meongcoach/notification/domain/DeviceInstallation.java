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
		name = "device_installations",
		uniqueConstraints = @UniqueConstraint(name = "uk_device_installations_fid", columnNames = "fid"),
		indexes = @Index(name = "idx_device_installations_user_id", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceInstallation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 512)
	private String fid;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DevicePlatform platform;

	@Column(nullable = false)
	private Instant lastSeenAt;

	public static DeviceInstallation register(Long userId, DeviceInstallationRegisterCommand command, Instant lastSeenAt) {
		DeviceInstallation deviceInstallation = new DeviceInstallation();

		deviceInstallation.userId = requireNonNull(userId);
		deviceInstallation.fid = requireNonNull(command.fid());
		deviceInstallation.platform = requireNonNull(command.platform());
		deviceInstallation.lastSeenAt = requireNonNull(lastSeenAt);

		return deviceInstallation;
	}

	public void renew(Long userId, Instant lastSeenAt) {
		this.userId = requireNonNull(userId);
		this.lastSeenAt = requireNonNull(lastSeenAt);
	}
}
