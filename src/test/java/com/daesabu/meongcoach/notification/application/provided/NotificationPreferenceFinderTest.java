package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceFixture;
import com.daesabu.meongcoach.notification.domain.SlotPreference;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class NotificationPreferenceFinderTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private NotificationPreferenceFinder notificationPreferenceFinder;

	@Autowired
	private NotificationPreferenceRepository notificationPreferenceRepository;

	@Test
	void 설정_행이_없으면_기본값을_반환하고_행을_만들지_않는다() {
		List<SlotPreference> slots = notificationPreferenceFinder.findSlotPreferences(USER_ID);

		assertThat(slots).containsExactly(
				new SlotPreference(NotificationSlot.MORNING, false),
				new SlotPreference(NotificationSlot.NOON, false),
				new SlotPreference(NotificationSlot.EVENING, true)
		);
		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID)).isEmpty();
	}

	@Test
	void 저장된_설정을_시간대_순서로_반환한다() {
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(USER_ID, NotificationSlot.EVENING, false));
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(USER_ID, NotificationSlot.MORNING, true));
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(USER_ID, NotificationSlot.NOON, false));

		List<SlotPreference> slots = notificationPreferenceFinder.findSlotPreferences(USER_ID);

		assertThat(slots).containsExactly(
				new SlotPreference(NotificationSlot.MORNING, true),
				new SlotPreference(NotificationSlot.NOON, false),
				new SlotPreference(NotificationSlot.EVENING, false)
		);
	}
}
