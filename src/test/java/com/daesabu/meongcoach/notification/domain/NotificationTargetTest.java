package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationTargetException;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class NotificationTargetTest {

	@Test
	void ALL은_회원_목록_없이_만든다() {
		assertThat(NotificationTarget.of("ALL", null).all()).isTrue();
	}

	@Test
	void USERS는_1000명까지_만든다() {
		List<Long> userIds = LongStream.rangeClosed(1, 1000).boxed().toList();

		assertThat(NotificationTarget.of("USERS", userIds).userIds()).hasSize(1000);
	}

	@Test
	void 잘못된_대상이면_던진다() {
		List<Long> tooMany = LongStream.rangeClosed(1, 1001).boxed().toList();

		assertThatThrownBy(() -> NotificationTarget.of("USERS", List.of())).isInstanceOf(InvalidNotificationTargetException.class);
		assertThatThrownBy(() -> NotificationTarget.of("USERS", tooMany)).isInstanceOf(InvalidNotificationTargetException.class);
		assertThatThrownBy(() -> NotificationTarget.of("ALL", List.of(1L))).isInstanceOf(InvalidNotificationTargetException.class);
		assertThatThrownBy(() -> NotificationTarget.of("SOME", List.of(1L))).isInstanceOf(InvalidNotificationTargetException.class);
	}
}
