package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
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
	private DeviceInstallationRepository deviceInstallationRepository;

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

	private void saveInstallation(Long userId, String fid, DevicePlatform platform) {
		deviceInstallationRepository.save(DeviceInstallation.register(userId, new DeviceInstallationRegisterCommand(fid, platform), NOW));
	}

	private void savePreference(Long userId, int hour, boolean enabled) {
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(userId, hour, enabled));
	}

	@Test
	void 시각을_켠_활성_회원의_FID을_한_행씩_반환한다() {
		Long userId = saveUser();
		savePreference(userId, 17, true);
		saveInstallation(userId, "fid-ios", DevicePlatform.IOS);
		saveInstallation(userId, "fid-android", DevicePlatform.ANDROID);

		List<NotificationTargetResult> targets = notificationTargetFinder.findTargets(17);

		assertThat(targets)
				.extracting(NotificationTargetResult::userId, NotificationTargetResult::fid,
						NotificationTargetResult::platform)
				.containsExactly(
						tuple(userId, "fid-ios", DevicePlatform.IOS),
						tuple(userId, "fid-android", DevicePlatform.ANDROID)
				);
	}

	@Test
	void 회원_ID_오름차순_같은_회원_안에서는_FID_등록_순으로_정렬한다() {
		Long firstUserId = saveUser();
		Long secondUserId = saveUser();
		savePreference(firstUserId, 17, true);
		savePreference(secondUserId, 17, true);
		saveInstallation(secondUserId, "second-ios", DevicePlatform.IOS);
		saveInstallation(firstUserId, "first-android", DevicePlatform.ANDROID);
		saveInstallation(firstUserId, "first-ios", DevicePlatform.IOS);

		List<NotificationTargetResult> targets = notificationTargetFinder.findTargets(17);

		assertThat(targets)
				.extracting(NotificationTargetResult::userId, NotificationTargetResult::fid)
				.containsExactly(
						tuple(firstUserId, "first-android"),
						tuple(firstUserId, "first-ios"),
						tuple(secondUserId, "second-ios")
				);
	}

	@Test
	void 시각을_끈_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, 17, false);
		saveInstallation(userId, "fid", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void 다른_시각만_켠_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, 8, true);
		saveInstallation(userId, "fid", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void 설정_행이_없는_회원은_기본값이_켜져_있어도_빠진다() {
		Long userId = saveUser();
		saveInstallation(userId, "fid", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void 탈퇴한_회원은_빠진다() {
		Long userId = saveWithdrawnUser();
		savePreference(userId, 17, true);
		saveInstallation(userId, "fid", DevicePlatform.IOS);

		assertThat(notificationTargetFinder.findTargets(17)).isEmpty();
	}

	@Test
	void FID이_없는_회원은_빠진다() {
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
