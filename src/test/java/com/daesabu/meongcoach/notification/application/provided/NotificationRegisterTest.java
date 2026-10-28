package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceTokenRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.HourPreferenceRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceTokenRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.DeviceToken;
import com.daesabu.meongcoach.notification.domain.DeviceTokenRegisterCommand;
import com.daesabu.meongcoach.notification.domain.NotificationPreference;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceFixture;
import com.daesabu.meongcoach.notification.domain.exception.InvalidDevicePlatformException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHoursException;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class NotificationRegisterTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 43L;
	private static final String TOKEN = "fcm-token-1";
	private static final String OTHER_TOKEN = "fcm-token-2";
	private static final Instant LAST_WEEK = Instant.parse("2026-10-21T00:00:00Z");

	private static final DeviceTokenRegisterRequest IOS_TOKEN = new DeviceTokenRegisterRequest(TOKEN, "IOS");

	@Autowired
	private NotificationRegister notificationRegister;

	@Autowired
	private DeviceTokenRepository deviceTokenRepository;

	@Autowired
	private NotificationPreferenceRepository notificationPreferenceRepository;

	private static NotificationPreferenceUpdateRequest hours(boolean at8, boolean at11, boolean at17) {
		return new NotificationPreferenceUpdateRequest(List.of(
				new HourPreferenceRequest(8, at8),
				new HourPreferenceRequest(11, at11),
				new HourPreferenceRequest(17, at17)
		));
	}

	private DeviceToken saveToken(Long userId, String token) {
		return deviceTokenRepository.save(
				DeviceToken.register(userId, new DeviceTokenRegisterCommand(token, DevicePlatform.ANDROID), LAST_WEEK));
	}

	private void savePreference(Long userId, int hour, boolean enabled) {
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(userId, hour, enabled));
	}

	@Test
	void 토큰을_등록하면_회원과_플랫폼과_마지막_확인_시각을_저장한다() {
		Instant before = Instant.now();

		notificationRegister.registerDeviceToken(USER_ID, IOS_TOKEN);

		assertThat(deviceTokenRepository.findByToken(TOKEN)).get().satisfies(deviceToken -> {
			assertThat(deviceToken.getUserId()).isEqualTo(USER_ID);
			assertThat(deviceToken.getPlatform()).isEqualTo(DevicePlatform.IOS);
			assertThat(deviceToken.getLastSeenAt()).isAfterOrEqualTo(before);
		});
	}

	@Test
	void 토큰을_처음_등록하면_기본_알림_설정_세_행을_만든다() {
		notificationRegister.registerDeviceToken(USER_ID, IOS_TOKEN);

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactlyInAnyOrder(
						tuple(8, false),
						tuple(11, false),
						tuple(17, true)
				);
	}

	@Test
	void 이미_알림_설정이_있으면_토큰을_등록해도_설정을_바꾸지_않는다() {
		savePreference(USER_ID, 17, false);

		notificationRegister.registerDeviceToken(USER_ID, IOS_TOKEN);

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactly(tuple(17, false));
	}

	@Test
	void 같은_토큰을_다른_회원이_등록하면_행을_새로_만들지_않고_회원을_옮긴다() {
		Long tokenId = saveToken(USER_ID, TOKEN).getId();

		notificationRegister.registerDeviceToken(OTHER_USER_ID, IOS_TOKEN);

		assertThat(deviceTokenRepository.findAll())
				.singleElement()
				.extracting(DeviceToken::getId, DeviceToken::getUserId)
				.containsExactly(tokenId, OTHER_USER_ID);
	}

	@Test
	void 같은_회원이_같은_토큰을_다시_등록하면_마지막_확인_시각만_갱신한다() {
		saveToken(USER_ID, TOKEN);

		notificationRegister.registerDeviceToken(USER_ID, IOS_TOKEN);

		assertThat(deviceTokenRepository.findAll())
				.singleElement()
				.satisfies(deviceToken -> {
					assertThat(deviceToken.getUserId()).isEqualTo(USER_ID);
					assertThat(deviceToken.getLastSeenAt()).isAfter(LAST_WEEK);
				});
	}

	@Test
	void 지원하지_않는_플랫폼이면_InvalidDevicePlatformException을_던진다() {
		DeviceTokenRegisterRequest request = new DeviceTokenRegisterRequest(TOKEN, "WEB");

		assertThatThrownBy(() -> notificationRegister.registerDeviceToken(USER_ID, request))
				.isInstanceOf(InvalidDevicePlatformException.class);
	}

	@Test
	void 본인_토큰을_지운다() {
		saveToken(USER_ID, TOKEN);
		saveToken(USER_ID, OTHER_TOKEN);

		notificationRegister.deleteDeviceToken(USER_ID, TOKEN);

		assertThat(deviceTokenRepository.findAll())
				.extracting(DeviceToken::getToken)
				.containsExactly(OTHER_TOKEN);
	}

	@Test
	void 다른_회원의_토큰은_지우지_않는다() {
		saveToken(OTHER_USER_ID, TOKEN);

		notificationRegister.deleteDeviceToken(USER_ID, TOKEN);

		assertThat(deviceTokenRepository.findByToken(TOKEN)).isPresent();
	}

	@Test
	void 없는_토큰을_지워도_예외가_없다() {
		assertThatCode(() -> notificationRegister.deleteDeviceToken(USER_ID, TOKEN))
				.doesNotThrowAnyException();
	}

	@Test
	void 무효_토큰_목록을_지우면_있는_토큰만_지우고_없는_토큰은_무시한다() {
		saveToken(USER_ID, TOKEN);
		saveToken(OTHER_USER_ID, OTHER_TOKEN);

		notificationRegister.deleteDeviceTokens(new DeviceTokenDeleteRequest(List.of(TOKEN, "unknown-token")));

		assertThat(deviceTokenRepository.findAll())
				.extracting(DeviceToken::getToken)
				.containsExactly(OTHER_TOKEN);
	}

	@Test
	void 알림_설정을_바꾸면_행이_없던_회원은_세_행이_새로_생긴다() {
		notificationRegister.updatePreferences(USER_ID, hours(true, false, false));

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactlyInAnyOrder(
						tuple(8, true),
						tuple(11, false),
						tuple(17, false)
				);
	}

	@Test
	void 알림_설정을_바꾸면_있던_행은_값만_바뀐다() {
		notificationRegister.updatePreferences(USER_ID, hours(false, false, true));
		List<Long> ids = notificationPreferenceRepository.findAllByUserId(USER_ID).stream()
				.map(NotificationPreference::getId)
				.toList();

		notificationRegister.updatePreferences(USER_ID, hours(true, true, false));

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getId)
				.containsExactlyInAnyOrderElementsOf(ids);
		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactlyInAnyOrder(
						tuple(8, true),
						tuple(11, true),
						tuple(17, false)
				);
	}

	@Test
	void 허용_시각이_빠지면_InvalidNotificationHoursException을_던진다() {
		NotificationPreferenceUpdateRequest request = new NotificationPreferenceUpdateRequest(List.of(
				new HourPreferenceRequest(8, true),
				new HourPreferenceRequest(11, true)
		));

		assertThatThrownBy(() -> notificationRegister.updatePreferences(USER_ID, request))
				.isInstanceOf(InvalidNotificationHoursException.class);
	}

	@Test
	void 시각이_0에서_23_밖이면_InvalidNotificationHourException을_던진다() {
		NotificationPreferenceUpdateRequest request = new NotificationPreferenceUpdateRequest(List.of(
				new HourPreferenceRequest(8, true),
				new HourPreferenceRequest(11, true),
				new HourPreferenceRequest(24, true)
		));

		assertThatThrownBy(() -> notificationRegister.updatePreferences(USER_ID, request))
				.isInstanceOf(InvalidNotificationHourException.class);
	}

	@Test
	void 회원의_토큰과_알림_설정을_모두_지우고_다른_회원은_건드리지_않는다() {
		saveToken(USER_ID, TOKEN);
		saveToken(OTHER_USER_ID, OTHER_TOKEN);
		savePreference(USER_ID, 17, true);
		savePreference(OTHER_USER_ID, 17, true);

		notificationRegister.deleteAllByUserId(USER_ID);

		assertThat(deviceTokenRepository.findAll()).extracting(DeviceToken::getUserId).containsExactly(OTHER_USER_ID);
		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID)).isEmpty();
		assertThat(notificationPreferenceRepository.findAllByUserId(OTHER_USER_ID)).hasSize(1);
	}
}
