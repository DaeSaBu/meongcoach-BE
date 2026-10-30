package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeviceInstallationTest {

	private static final Long USER_ID = 1L;
	private static final Long OTHER_USER_ID = 2L;
	private static final String TOKEN = "fcm-fid";
	private static final Instant NOW = Instant.parse("2026-10-28T08:00:00Z");
	private static final Instant LATER = NOW.plus(Duration.ofHours(1));

	@Test
	void 등록하면_회원과_FID과_플랫폼과_마지막_확인_시각을_기록한다() {
		DeviceInstallation deviceInstallation = DeviceInstallation.register(USER_ID,
				new DeviceInstallationRegisterCommand(TOKEN, DevicePlatform.IOS), NOW);

		assertThat(deviceInstallation.getUserId()).isEqualTo(USER_ID);
		assertThat(deviceInstallation.getFid()).isEqualTo(TOKEN);
		assertThat(deviceInstallation.getPlatform()).isEqualTo(DevicePlatform.IOS);
		assertThat(deviceInstallation.getLastSeenAt()).isEqualTo(NOW);
	}

	@Test
	void 갱신하면_회원과_마지막_확인_시각을_바꾼다() {
		DeviceInstallation deviceInstallation = DeviceInstallation.register(USER_ID,
				new DeviceInstallationRegisterCommand(TOKEN, DevicePlatform.ANDROID), NOW);

		deviceInstallation.renew(OTHER_USER_ID, LATER);

		assertThat(deviceInstallation.getUserId()).isEqualTo(OTHER_USER_ID);
		assertThat(deviceInstallation.getLastSeenAt()).isEqualTo(LATER);
		assertThat(deviceInstallation.getFid()).isEqualTo(TOKEN);
		assertThat(deviceInstallation.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
	}
}
