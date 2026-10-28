package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.notification.application.required.DeviceTokenRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.DeviceToken;
import com.daesabu.meongcoach.notification.domain.DeviceTokenRegisterCommand;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceFixture;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class NotificationTargetFinderTest {

	private static final Instant NOW = Instant.parse("2026-10-28T08:00:00Z");

	@Autowired
	private NotificationTargetFinder notificationTargetFinder;

	@Autowired
	private DeviceTokenRepository deviceTokenRepository;

	@Autowired
	private NotificationPreferenceRepository notificationPreferenceRepository;

	@Autowired
	private UserRepository userRepository;

	private Long saveUser() {
		return userRepository.save(User.registerUser()).getId();
	}

	private Long saveWithdrawnUser() {
		User user = User.registerUser();
		user.withdraw();
		return userRepository.save(user).getId();
	}

	private void saveToken(Long userId, String token, DevicePlatform platform) {
		deviceTokenRepository.save(DeviceToken.register(userId, new DeviceTokenRegisterCommand(token, platform), NOW));
	}

	private void savePreference(Long userId, int hour, boolean enabled) {
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(userId, hour, enabled));
	}

	@Test
	void 시각을_켠_활성_회원의_토큰을_회원별로_묶어_반환한다() {
		Long userId = saveUser();
		savePreference(userId, 17, true);
		saveToken(userId, "token-ios", DevicePlatform.IOS);
		saveToken(userId, "token-android", DevicePlatform.ANDROID);

		List<NotificationTargetResult> targets = notificationTargetFinder.findTargets(17);

		assertThat(targets).singleElement().satisfies(target -> {
			assertThat(target.userId()).isEqualTo(userId);
			assertThat(target.tokens())
					.extracting(DeviceTokenResult::token, DeviceTokenResult::platform)
					.containsExactlyInAnyOrder(
							tuple("token-ios", DevicePlatform.IOS),
							tuple("token-android", DevicePlatform.ANDROID)
					);
		});
	}

	@Test
	void 시각을_끈_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, 17, false);
		saveToken(userId, "token", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void 다른_시각만_켠_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, 8, true);
		saveToken(userId, "token", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void 설정_행이_없는_회원은_기본값이_켜져_있어도_빠진다() {
		Long userId = saveUser();
		saveToken(userId, "token", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void 탈퇴한_회원은_빠진다() {
		Long userId = saveWithdrawnUser();
		savePreference(userId, 17, true);
		saveToken(userId, "token", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void 토큰이_없는_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, 17, true);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void 시각이_0에서_23_밖이면_InvalidNotificationHourException을_던진다() {
		assertThatThrownBy(() -> notificationTargetFinder.findTargets(24))
				.isInstanceOf(InvalidNotificationHourException.class);
	}
}
