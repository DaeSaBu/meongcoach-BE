package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeviceTokenTest {

	private static final Long USER_ID = 1L;
	private static final Long OTHER_USER_ID = 2L;
	private static final String TOKEN = "fcm-token";
	private static final Instant NOW = Instant.parse("2026-10-28T08:00:00Z");
	private static final Instant LATER = NOW.plus(Duration.ofHours(1));

	@Test
	void 등록하면_회원과_토큰과_플랫폼과_마지막_확인_시각을_기록한다() {
		DeviceToken deviceToken = DeviceToken.register(USER_ID,
				new DeviceTokenRegisterCommand(TOKEN, DevicePlatform.IOS), NOW);

		assertThat(deviceToken.getUserId()).isEqualTo(USER_ID);
		assertThat(deviceToken.getToken()).isEqualTo(TOKEN);
		assertThat(deviceToken.getPlatform()).isEqualTo(DevicePlatform.IOS);
		assertThat(deviceToken.getLastSeenAt()).isEqualTo(NOW);
	}

	@Test
	void 갱신하면_회원과_마지막_확인_시각을_바꾼다() {
		DeviceToken deviceToken = DeviceToken.register(USER_ID,
				new DeviceTokenRegisterCommand(TOKEN, DevicePlatform.ANDROID), NOW);

		deviceToken.renew(OTHER_USER_ID, LATER);

		assertThat(deviceToken.getUserId()).isEqualTo(OTHER_USER_ID);
		assertThat(deviceToken.getLastSeenAt()).isEqualTo(LATER);
		assertThat(deviceToken.getToken()).isEqualTo(TOKEN);
		assertThat(deviceToken.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
	}
}
