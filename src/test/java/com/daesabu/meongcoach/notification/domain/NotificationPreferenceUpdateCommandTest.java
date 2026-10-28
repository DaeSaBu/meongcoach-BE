package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationSlotsException;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationPreferenceUpdateCommandTest {

	@Test
	void 세_슬롯이_한_번씩_있으면_슬롯별_설정을_돌려준다() {
		NotificationPreferenceUpdateCommand command = new NotificationPreferenceUpdateCommand(List.of(
				new SlotPreference(NotificationSlot.EVENING, false),
				new SlotPreference(NotificationSlot.MORNING, true),
				new SlotPreference(NotificationSlot.NOON, false)
		));

		assertThat(command.isEnabled(NotificationSlot.MORNING)).isTrue();
		assertThat(command.isEnabled(NotificationSlot.NOON)).isFalse();
		assertThat(command.isEnabled(NotificationSlot.EVENING)).isFalse();
	}

	@Test
	void 슬롯이_빠지면_InvalidNotificationSlotsException을_던진다() {
		List<SlotPreference> slots = List.of(
				new SlotPreference(NotificationSlot.MORNING, true),
				new SlotPreference(NotificationSlot.NOON, true)
		);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(slots))
				.isInstanceOf(InvalidNotificationSlotsException.class);
	}

	@Test
	void 슬롯이_중복되면_InvalidNotificationSlotsException을_던진다() {
		List<SlotPreference> slots = List.of(
				new SlotPreference(NotificationSlot.MORNING, true),
				new SlotPreference(NotificationSlot.MORNING, false),
				new SlotPreference(NotificationSlot.EVENING, true)
		);

		assertThatThrownBy(() -> new NotificationPreferenceUpdateCommand(slots))
				.isInstanceOf(InvalidNotificationSlotsException.class);
	}
}
