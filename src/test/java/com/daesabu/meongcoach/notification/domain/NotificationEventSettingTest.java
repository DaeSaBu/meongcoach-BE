package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationEventTypeException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationEventSettingTest {

	@Test
	void 기본_설정은_모든_종류를_켠다() {
		List<NotificationEventSetting> defaults = NotificationEventSetting.createDefaults(1L);

		assertThat(defaults).extracting(NotificationEventSetting::getType)
				.containsExactlyInAnyOrder(NotificationEventType.values());
		assertThat(defaults).allMatch(NotificationEventSetting::isEnabled);
	}

	@Test
	void 수신_여부를_바꾼다() {
		NotificationEventSetting setting = NotificationEventSetting.create(1L, NotificationEventType.COMMENT, true);

		setting.change(false);

		assertThat(setting.isEnabled()).isFalse();
	}

	@Test
	void 없는_종류면_InvalidNotificationEventTypeException을_던진다() {
		assertThatThrownBy(() -> NotificationEventType.from("MARKETING"))
				.isInstanceOf(InvalidNotificationEventTypeException.class);
	}
}
