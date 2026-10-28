package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationPreferencesTest {

	private static final Long USER_ID = 1L;

	private static NotificationPreferenceUpdateCommand allOn() {
		return new NotificationPreferenceUpdateCommand(List.of(
				new HourPreference(8, true),
				new HourPreference(11, true),
				new HourPreference(17, true)
		));
	}

	@Test
	void 설정_행이_없으면_비어_있다() {
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of());

		assertThat(preferences.isEmpty()).isTrue();
	}

	@Test
	void 기본_설정을_만들면_8시와_11시는_꺼지고_17시는_켜진다() {
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of());

		List<NotificationPreference> created = preferences.createDefaults();

		assertThat(created)
				.extracting(NotificationPreference::getUserId, NotificationPreference::getHour,
						NotificationPreference::isEnabled)
				.containsExactly(
						tuple(USER_ID, 8, false),
						tuple(USER_ID, 11, false),
						tuple(USER_ID, 17, true)
				);
	}

	@Test
	void 이미_설정_행이_있으면_기본_설정을_만들_수_없다() {
		NotificationPreference existing = NotificationPreference.create(USER_ID, 8, true);
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of(existing));

		assertThatThrownBy(preferences::createDefaults)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 요청대로_바꾸면_있던_행은_그_자리에서_바뀌고_없던_시각만_새로_만든다() {
		NotificationPreference morning = NotificationPreference.create(USER_ID, 8, false);
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of(morning));

		List<NotificationPreference> created = preferences.apply(allOn());

		assertThat(morning.isEnabled()).isTrue();
		assertThat(created)
				.extracting(NotificationPreference::getUserId, NotificationPreference::getHour,
						NotificationPreference::isEnabled)
				.containsExactly(
						tuple(USER_ID, 11, true),
						tuple(USER_ID, 17, true)
				);
	}

	@Test
	void 시각별_설정은_행이_없으면_기본값이다() {
		NotificationPreference evening = NotificationPreference.create(USER_ID, 17, false);
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of(evening));

		List<HourPreference> hourPreferences = preferences.hourPreferences();

		assertThat(hourPreferences).containsExactly(
				new HourPreference(8, false),
				new HourPreference(11, false),
				new HourPreference(17, false)
		);
	}
}
