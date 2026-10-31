package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationUnregisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationPreferenceUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationPreference;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceFixture;
import com.daesabu.meongcoach.notification.domain.exception.InvalidDevicePlatformException;
import com.daesabu.meongcoach.notification.domain.exception.NotAllowedNotificationHourException;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class NotificationRegisterTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 43L;
	private static final String TOKEN = "fcm-fid-1";
	private static final String OTHER_TOKEN = "fcm-fid-2";
	private static final Instant LAST_WEEK = Instant.parse("2026-10-21T00:00:00Z");

	private static final DeviceInstallationRegisterRequest IOS_TOKEN = new DeviceInstallationRegisterRequest(TOKEN, "IOS");

	@Autowired
	private NotificationRegister notificationRegister;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private NotificationPreferenceRepository notificationPreferenceRepository;

	private static final NotificationPreferenceUpdateRequest ON = new NotificationPreferenceUpdateRequest(true);
	private static final NotificationPreferenceUpdateRequest OFF = new NotificationPreferenceUpdateRequest(false);

	private DeviceInstallation saveInstallation(Long userId, String fid) {
		return deviceInstallationRepository.save(
				DeviceInstallation.register(userId, new DeviceInstallationRegisterCommand(fid, DevicePlatform.ANDROID), LAST_WEEK));
	}

	private void savePreference(Long userId, int hour, boolean enabled) {
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(userId, hour, enabled));
	}

	@Test
	void FID을_등록하면_회원과_플랫폼과_마지막_확인_시각을_저장한다() {
		Instant before = Instant.now();

		notificationRegister.registerDeviceInstallation(USER_ID, IOS_TOKEN);

		assertThat(deviceInstallationRepository.findByFid(TOKEN)).get().satisfies(deviceInstallation -> {
			assertThat(deviceInstallation.getUserId()).isEqualTo(USER_ID);
			assertThat(deviceInstallation.getPlatform()).isEqualTo(DevicePlatform.IOS);
			assertThat(deviceInstallation.getLastSeenAt()).isAfterOrEqualTo(before);
		});
	}

	@Test
	void FID을_처음_등록하면_기본_알림_설정_세_행을_만든다() {
		notificationRegister.registerDeviceInstallation(USER_ID, IOS_TOKEN);

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactlyInAnyOrder(
						tuple(8, false),
						tuple(11, false),
						tuple(17, true)
				);
	}

	@Test
	void 이미_알림_설정이_있으면_FID을_등록해도_설정을_바꾸지_않는다() {
		savePreference(USER_ID, 17, false);

		notificationRegister.registerDeviceInstallation(USER_ID, IOS_TOKEN);

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactlyInAnyOrder(tuple(8, false), tuple(11, false), tuple(17, false));
	}

	@Test
	void 알림_설정을_먼저_변경해도_FID_등록_시_누락된_시각의_기본값을_만든다() {
		notificationRegister.updatePreference(USER_ID, 8, ON);

		notificationRegister.registerDeviceInstallation(USER_ID, IOS_TOKEN);
		notificationRegister.registerDeviceInstallation(USER_ID, IOS_TOKEN);

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactlyInAnyOrder(tuple(8, true), tuple(11, false), tuple(17, true));
	}

	@Test
	void 같은_FID을_다른_회원이_등록하면_행을_새로_만들지_않고_회원을_옮긴다() {
		Long tokenId = saveInstallation(USER_ID, TOKEN).getId();

		notificationRegister.registerDeviceInstallation(OTHER_USER_ID, IOS_TOKEN);

		assertThat(deviceInstallationRepository.findAll())
				.singleElement()
				.extracting(DeviceInstallation::getId, DeviceInstallation::getUserId)
				.containsExactly(tokenId, OTHER_USER_ID);
	}

	@Test
	void 같은_회원이_같은_FID을_다시_등록하면_마지막_확인_시각만_갱신한다() {
		saveInstallation(USER_ID, TOKEN);

		notificationRegister.registerDeviceInstallation(USER_ID, IOS_TOKEN);

		assertThat(deviceInstallationRepository.findAll())
				.singleElement()
				.satisfies(deviceInstallation -> {
					assertThat(deviceInstallation.getUserId()).isEqualTo(USER_ID);
					assertThat(deviceInstallation.getLastSeenAt()).isAfter(LAST_WEEK);
				});
	}

	@Test
	void 지원하지_않는_플랫폼이면_InvalidDevicePlatformException을_던진다() {
		DeviceInstallationRegisterRequest request = new DeviceInstallationRegisterRequest(TOKEN, "WEB");

		assertThatThrownBy(() -> notificationRegister.registerDeviceInstallation(USER_ID, request))
				.isInstanceOf(InvalidDevicePlatformException.class);
	}

	@Test
	void 본인_FID을_지운다() {
		saveInstallation(USER_ID, TOKEN);
		saveInstallation(USER_ID, OTHER_TOKEN);

		notificationRegister.deleteDeviceInstallation(USER_ID, new DeviceInstallationUnregisterRequest(TOKEN));

		assertThat(deviceInstallationRepository.findAll())
				.extracting(DeviceInstallation::getFid)
				.containsExactly(OTHER_TOKEN);
	}

	@Test
	void 다른_회원의_FID은_지우지_않는다() {
		saveInstallation(OTHER_USER_ID, TOKEN);

		notificationRegister.deleteDeviceInstallation(USER_ID, new DeviceInstallationUnregisterRequest(TOKEN));

		assertThat(deviceInstallationRepository.findByFid(TOKEN)).isPresent();
	}

	@Test
	void 없는_FID을_지워도_예외가_없다() {
		assertThatCode(() -> notificationRegister.deleteDeviceInstallation(USER_ID, new DeviceInstallationUnregisterRequest(TOKEN)))
				.doesNotThrowAnyException();
	}

	@Test
	void 행이_없는_시각을_켜면_행을_새로_만든다() {
		notificationRegister.updatePreference(USER_ID, 8, ON);

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactly(tuple(8, true));
	}

	@Test
	void 행이_있는_시각을_끄면_행을_새로_만들지_않고_값만_바꾼다() {
		savePreference(USER_ID, 17, true);
		Long id = notificationPreferenceRepository.findAllByUserId(USER_ID).getFirst().getId();

		notificationRegister.updatePreference(USER_ID, 17, OFF);

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getId, NotificationPreference::getHour,
						NotificationPreference::isEnabled)
				.containsExactly(tuple(id, 17, false));
	}

	@Test
	void 다른_시각과_다른_회원의_행은_바꾸지_않는다() {
		savePreference(USER_ID, 8, false);
		savePreference(OTHER_USER_ID, 11, false);

		notificationRegister.updatePreference(USER_ID, 11, ON);

		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactlyInAnyOrder(tuple(8, false), tuple(11, true));
		assertThat(notificationPreferenceRepository.findAllByUserId(OTHER_USER_ID))
				.extracting(NotificationPreference::getHour, NotificationPreference::isEnabled)
				.containsExactly(tuple(11, false));
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 9, 24})
	void 허용_시각이_아니면_NotAllowedNotificationHourException을_던진다(int hour) {
		assertThatThrownBy(() -> notificationRegister.updatePreference(USER_ID, hour, ON))
				.isInstanceOf(NotAllowedNotificationHourException.class);
		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID)).isEmpty();
	}

	@Test
	void 회원의_FID과_알림_설정을_모두_지우고_다른_회원은_건드리지_않는다() {
		saveInstallation(USER_ID, TOKEN);
		saveInstallation(OTHER_USER_ID, OTHER_TOKEN);
		savePreference(USER_ID, 17, true);
		savePreference(OTHER_USER_ID, 17, true);

		notificationRegister.deleteAllByUserId(USER_ID);

		assertThat(deviceInstallationRepository.findAll()).extracting(DeviceInstallation::getUserId).containsExactly(OTHER_USER_ID);
		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID)).isEmpty();
		assertThat(notificationPreferenceRepository.findAllByUserId(OTHER_USER_ID)).hasSize(1);
	}
}
