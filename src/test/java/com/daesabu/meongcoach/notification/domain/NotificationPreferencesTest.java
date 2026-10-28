package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationPreferencesTest {

	private static final Long USER_ID = 1L;

	private static NotificationPreferenceUpdateCommand allOn() {
		return new NotificationPreferenceUpdateCommand(List.of(
				new SlotPreference(NotificationSlot.MORNING, true),
				new SlotPreference(NotificationSlot.NOON, true),
				new SlotPreference(NotificationSlot.EVENING, true)
		));
	}

	@Test
	void 설정_행이_없으면_비어_있다() {
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of());

		assertThat(preferences.isEmpty()).isTrue();
	}

	@Test
	void 기본_설정을_만들면_아침과_점심은_꺼지고_저녁은_켜진다() {
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of());

		List<NotificationPreference> created = preferences.createDefaults();

		assertThat(created)
				.extracting(NotificationPreference::getUserId, NotificationPreference::getSlot,
						NotificationPreference::isEnabled)
				.containsExactly(
						tuple(USER_ID, NotificationSlot.MORNING, false),
						tuple(USER_ID, NotificationSlot.NOON, false),
						tuple(USER_ID, NotificationSlot.EVENING, true)
				);
	}

	@Test
	void 이미_설정_행이_있으면_기본_설정을_만들_수_없다() {
		NotificationPreference existing = NotificationPreference.create(USER_ID, NotificationSlot.MORNING, true);
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of(existing));

		assertThatThrownBy(preferences::createDefaults)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 요청대로_바꾸면_있던_행은_그_자리에서_바뀌고_없던_슬롯만_새로_만든다() {
		NotificationPreference morning = NotificationPreference.create(USER_ID, NotificationSlot.MORNING, false);
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of(morning));

		List<NotificationPreference> created = preferences.apply(allOn());

		assertThat(morning.isEnabled()).isTrue();
		assertThat(created)
				.extracting(NotificationPreference::getUserId, NotificationPreference::getSlot,
						NotificationPreference::isEnabled)
				.containsExactly(
						tuple(USER_ID, NotificationSlot.NOON, true),
						tuple(USER_ID, NotificationSlot.EVENING, true)
				);
	}

	@Test
	void 슬롯별_설정은_행이_없으면_기본값이다() {
		NotificationPreference evening = NotificationPreference.create(USER_ID, NotificationSlot.EVENING, false);
		NotificationPreferences preferences = new NotificationPreferences(USER_ID, List.of(evening));

		List<SlotPreference> slotPreferences = preferences.slotPreferences();

		assertThat(slotPreferences).containsExactly(
				new SlotPreference(NotificationSlot.MORNING, false),
				new SlotPreference(NotificationSlot.NOON, false),
				new SlotPreference(NotificationSlot.EVENING, false)
		);
	}
}
