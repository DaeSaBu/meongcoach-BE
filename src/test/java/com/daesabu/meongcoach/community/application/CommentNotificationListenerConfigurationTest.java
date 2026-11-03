package com.daesabu.meongcoach.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CommentNotificationListenerConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withBean(UserFinder.class, () -> mock(UserFinder.class))
			.withBean(EventNotificationDispatcher.class, () -> mock(EventNotificationDispatcher.class))
			.withUserConfiguration(CommentNotificationListener.class);

	@Test
	void 댓글_알림을_켜면_리스너를_등록한다() {
		runner.withPropertyValues("meongcoach.notification.comment.enabled=true")
				.run(context -> assertThat(context).hasSingleBean(CommentNotificationListener.class));
	}

	@Test
	void 댓글_알림을_끄면_리스너를_등록하지_않는다() {
		runner.withPropertyValues("meongcoach.notification.comment.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(CommentNotificationListener.class));
	}
}
