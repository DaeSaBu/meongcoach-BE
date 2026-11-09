package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.domain.AiAnalysisCompletedEvent;
import com.daesabu.meongcoach.notification.application.provided.EventNotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiAnalysisNotificationListener {

	private final EventNotificationDispatcher eventNotificationDispatcher;

	@Async
	@EventListener
	public void notify(AiAnalysisCompletedEvent event) {
		eventNotificationDispatcher.dispatch(new EventNotificationDispatchRequest(
				"AI_ANALYSIS", "USERS", List.of(event.userId()), "AI 분석이 완료됐어요",
				"분석 결과를 확인해 보세요.", "meongcoach://analysis"));
	}
}
