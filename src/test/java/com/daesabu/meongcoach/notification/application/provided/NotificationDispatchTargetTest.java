package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceFixture;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class NotificationDispatchTargetTest {

	private static final Instant NOW = Instant.parse("2026-10-28T08:00:00Z");

	@Autowired
	private NotificationDispatcher dispatcher;

	@Autowired
	private NotificationSender sender;

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

		List<String> targets = dispatchTargets(17);

		assertThat(targets).containsExactly("fid-ios", "fid-android");
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

		List<String> targets = dispatchTargets(17);

		assertThat(targets).containsExactly("first-android", "first-ios", "second-ios");
	}

	@Test
	void 시각을_끈_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, 17, false);
		saveInstallation(userId, "fid", DevicePlatform.IOS);

		assertThat(dispatchTargets(17)).isEmpty();
	}

	@Test
	void 다른_시각만_켠_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, 8, true);
		saveInstallation(userId, "fid", DevicePlatform.IOS);

		assertThat(dispatchTargets(17)).isEmpty();
	}

	@Test
	void 설정_행이_없는_회원은_기본값이_켜져_있어도_빠진다() {
		Long userId = saveUser();
		saveInstallation(userId, "fid", DevicePlatform.IOS);

		assertThat(dispatchTargets(17)).isEmpty();
	}

	@Test
	void 탈퇴한_회원은_빠진다() {
		Long userId = saveWithdrawnUser();
		savePreference(userId, 17, true);
		saveInstallation(userId, "fid", DevicePlatform.IOS);

		assertThat(dispatchTargets(17)).isEmpty();
	}

	@Test
	void FID이_없는_회원은_빠진다() {
		Long userId = saveUser();
		savePreference(userId, 17, true);

		assertThat(dispatchTargets(17)).isEmpty();
	}

	@Test
	void 시각이_0에서_23_밖이면_InvalidNotificationHourException을_던진다() {
		assertThatThrownBy(() -> dispatchTargets(24))
				.isInstanceOf(InvalidNotificationHourException.class);
	}
	private List<String> dispatchTargets(int hour) {
		List<String> fids = new ArrayList<>();
		given(sender.send(anyMap())).willAnswer(invocation -> {
			Map<String, NotificationMessage> messages = invocation.getArgument(0);
			fids.addAll(messages.keySet());
			return List.of();
		});
		dispatcher.dispatch(new NotificationDispatchRequest(NOW, hour));
		return fids;
	}

}
