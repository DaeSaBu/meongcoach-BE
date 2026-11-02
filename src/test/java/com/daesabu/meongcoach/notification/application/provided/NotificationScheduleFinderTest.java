package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.notification.application.required.NotificationScheduleRepository;
import com.daesabu.meongcoach.notification.domain.NotificationScheduleFixture;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class NotificationScheduleFinderTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private NotificationScheduleFinder notificationScheduleFinder;

	@Autowired
	private NotificationScheduleRepository notificationScheduleRepository;

	@Test
	void 설정_행이_없으면_빈_목록을_반환하고_행을_만들지_않는다() {
		List<NotificationScheduleResult> schedules = notificationScheduleFinder.findSchedules(USER_ID);

		assertThat(schedules).isEmpty();
		assertThat(notificationScheduleRepository.findAllByUserId(USER_ID)).isEmpty();
	}

	@Test
	void 저장된_설정을_시와_분_순서로_반환한다() {
		notificationScheduleRepository.save(NotificationScheduleFixture.create(USER_ID, 17, false));
		notificationScheduleRepository.save(NotificationScheduleFixture.create(USER_ID, 8, true));
		notificationScheduleRepository.save(NotificationScheduleFixture.create(USER_ID, 11, false));
		notificationScheduleRepository.save(NotificationScheduleFixture.create(USER_ID, 8, 30, false));

		List<NotificationScheduleResult> schedules = notificationScheduleFinder.findSchedules(USER_ID);

		assertThat(schedules).containsExactly(
				new NotificationScheduleResult(8, 0, true),
				new NotificationScheduleResult(8, 30, false),
				new NotificationScheduleResult(11, 0, false),
				new NotificationScheduleResult(17, 0, false)
		);
	}
}
