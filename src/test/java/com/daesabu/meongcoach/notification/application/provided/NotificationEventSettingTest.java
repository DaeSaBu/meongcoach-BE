package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationEventSettingUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.domain.NotificationEventSetting;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class NotificationEventSettingTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private NotificationRegister register;

	@Autowired
	private NotificationEventSettingFinder finder;

	@Autowired
	private NotificationEventSettingRepository repository;

	@Test
	void FID을_등록하면_모든_종류의_설정을_만들고_다시_등록해도_값을_유지한다() {
		var fid = new DeviceInstallationRegisterRequest("event-setting-fid", "IOS");
		register.registerDeviceInstallation(USER_ID, fid);
		register.updateEventSetting(USER_ID, new NotificationEventSettingUpdateRequest("COMMENT", false));

		register.registerDeviceInstallation(USER_ID, fid);

		assertThat(repository.findAllByUserId(USER_ID))
				.extracting(NotificationEventSetting::getType, NotificationEventSetting::isEnabled)
				.containsExactlyInAnyOrder(
						tuple(NotificationEventType.AI_ANALYSIS, true),
						tuple(NotificationEventType.OPERATOR_MESSAGE, true),
						tuple(NotificationEventType.COMMENT, false));
	}

	@Test
	void 행이_없는_종류를_바꾸면_새로_만들고_있으면_값만_바꾼다() {
		register.updateEventSetting(USER_ID, new NotificationEventSettingUpdateRequest("AI_ANALYSIS", false));
		register.updateEventSetting(USER_ID, new NotificationEventSettingUpdateRequest("AI_ANALYSIS", true));

		assertThat(finder.findEventSettings(USER_ID))
				.containsExactly(new NotificationEventSettingResult(NotificationEventType.AI_ANALYSIS, true));
	}

	@Test
	void 탈퇴하면_이벤트_설정도_지운다() {
		register.updateEventSetting(USER_ID, new NotificationEventSettingUpdateRequest("COMMENT", true));

		register.deleteAllByUserId(USER_ID);

		assertThat(repository.findAllByUserId(USER_ID)).isEmpty();
	}
}
