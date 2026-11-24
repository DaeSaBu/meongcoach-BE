package com.daesabu.meongcoach.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.daesabu.meongcoach.community.application.provided.BlockFinder;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LikeNotificationListenerConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withBean(EventNotificationDispatcher.class, () -> mock(EventNotificationDispatcher.class))
			.withBean(BlockFinder.class, () -> mock(BlockFinder.class))
			.withUserConfiguration(LikeNotificationListener.class);

	@Test
	void 좋아요_알림을_켜면_리스너를_등록한다() {
		runner.withPropertyValues("meongcoach.notification.like.enabled=true")
				.run(context -> assertThat(context).hasSingleBean(LikeNotificationListener.class));
	}

	@Test
	void 좋아요_알림을_끄면_리스너를_등록하지_않는다() {
		runner.withPropertyValues("meongcoach.notification.like.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(LikeNotificationListener.class));
	}
}
