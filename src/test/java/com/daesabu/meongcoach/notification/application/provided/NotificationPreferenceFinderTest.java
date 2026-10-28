package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.notification.application.required.NotificationPreferenceRepository;
import com.daesabu.meongcoach.notification.domain.NotificationPreferenceFixture;
import com.daesabu.meongcoach.notification.domain.HourPreference;
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
		List<HourPreference> hours = notificationPreferenceFinder.findHourPreferences(USER_ID);

		assertThat(hours).containsExactly(
				new HourPreference(8, false),
				new HourPreference(11, false),
				new HourPreference(17, true)
		);
		assertThat(notificationPreferenceRepository.findAllByUserId(USER_ID)).isEmpty();
	}

	@Test
	void 저장된_설정을_시각_순서로_반환한다() {
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(USER_ID, 17, false));
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(USER_ID, 8, true));
		notificationPreferenceRepository.save(NotificationPreferenceFixture.create(USER_ID, 11, false));

		List<HourPreference> hours = notificationPreferenceFinder.findHourPreferences(USER_ID);

		assertThat(hours).containsExactly(
				new HourPreference(8, true),
				new HourPreference(11, false),
				new HourPreference(17, false)
		);
	}
}
