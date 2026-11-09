package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.daesabu.meongcoach.ai.domain.AiAnalysisCompletedEvent;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

class AiAnalysisNotificationListenerTest {

	@Test
	void 소유자에게_분석_화면_알림을_비동기로_보내고_발송_실패는_호출자에게_전파하지_않는다() throws Exception {
		var received = new CompletableFuture<EventNotificationDispatchRequest>();
		var worker = new CompletableFuture<Thread>();
		var dispatcher = mock(EventNotificationDispatcher.class);
		doAnswer(invocation -> {
			worker.complete(Thread.currentThread());
			received.complete(invocation.getArgument(0));
			throw new IllegalStateException("발송 실패");
		}).when(dispatcher).dispatch(any());

		try (var context = new AnnotationConfigApplicationContext()) {
			context.register(AsyncConfig.class, AiAnalysisNotificationListener.class);
			context.registerBean(EventNotificationDispatcher.class, () -> dispatcher);
			context.refresh();

			context.publishEvent(new AiAnalysisCompletedEvent(7L));

			assertThat(received.get(5, TimeUnit.SECONDS)).isEqualTo(new EventNotificationDispatchRequest(
					"AI_ANALYSIS", "USERS", java.util.List.of(7L), "AI 분석이 완료됐어요",
					"분석 결과를 확인해 보세요.", "meongcoach://analysis"));
			assertThat(worker.get(5, TimeUnit.SECONDS)).isNotSameAs(Thread.currentThread());
		}
	}

	@Configuration
	@EnableAsync
	static class AsyncConfig {
	}
}
