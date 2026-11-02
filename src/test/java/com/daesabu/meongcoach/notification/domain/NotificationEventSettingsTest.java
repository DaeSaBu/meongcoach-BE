package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationEventSettingsTest {

	private static final Long USER_ID = 1L;

	@Test
	void 설정이_없으면_모든_종류의_기본_설정을_만든다() {
		NotificationEventSettings settings = new NotificationEventSettings(List.of());

		List<NotificationEventSetting> created = settings.createMissingDefaults(USER_ID);

		assertThat(created).extracting(NotificationEventSetting::getType)
				.containsExactlyInAnyOrder(NotificationEventType.values());
	}

	@Test
	void 이미_있는_종류는_빼고_없는_종류만_만든다() {
		NotificationEventSetting existing = NotificationEventSetting.create(USER_ID, NotificationEventType.COMMENT, false);
		NotificationEventSettings settings = new NotificationEventSettings(List.of(existing));

		List<NotificationEventSetting> created = settings.createMissingDefaults(USER_ID);

		assertThat(created).extracting(NotificationEventSetting::getType)
				.doesNotContain(NotificationEventType.COMMENT)
				.hasSize(NotificationEventType.values().length - 1);
	}

	@Test
	void 모든_종류가_있으면_만들지_않는다() {
		NotificationEventSettings settings = new NotificationEventSettings(NotificationEventSetting.createDefaults(USER_ID));

		assertThat(settings.createMissingDefaults(USER_ID)).isEmpty();
	}
}
