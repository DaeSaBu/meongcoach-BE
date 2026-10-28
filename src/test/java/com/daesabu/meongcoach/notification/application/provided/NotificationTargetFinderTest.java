package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.notification.application.required.DeviceTokenRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.DeviceToken;
import com.daesabu.meongcoach.notification.domain.DeviceTokenRegisterCommand;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceFixture;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
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

	private void savePreference(Long userId, NotificationSlot slot, boolean enabled) {
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(userId, slot, enabled));
	}

	@Test
	void 시간대를_켠_활성_회원의_토큰을_회원별로_묶어_반환한다() {
		Long userId = saveUser();
		savePreference(userId, NotificationSlot.EVENING, true);
		saveToken(userId, "token-ios", DevicePlatform.IOS);
		saveToken(userId, "token-android", DevicePlatform.ANDROID);

		List<NotificationTargetResult> targets = notificationTargetFinder.findTargets(NotificationSlot.EVENING);

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
	void 시간대를_끈_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, NotificationSlot.EVENING, false);
		saveToken(userId, "token", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(NotificationSlot.EVENING)).isEmpty();
	}

	@Test
	void 다른_시간대만_켠_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, NotificationSlot.MORNING, true);
		saveToken(userId, "token", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(NotificationSlot.EVENING)).isEmpty();
	}

	@Test
	void 설정_행이_없는_회원은_기본값이_켜져_있어도_빠진다() {
		Long userId = saveUser();
		saveToken(userId, "token", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(NotificationSlot.EVENING)).isEmpty();
	}

	@Test
	void 탈퇴한_회원은_빠진다() {
		Long userId = saveWithdrawnUser();
		savePreference(userId, NotificationSlot.EVENING, true);
		saveToken(userId, "token", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(NotificationSlot.EVENING)).isEmpty();
	}

	@Test
	void 토큰이_없는_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, NotificationSlot.EVENING, true);

		assertThat(notificationTargetFinder.findTargets(NotificationSlot.EVENING)).isEmpty();
	}
}
