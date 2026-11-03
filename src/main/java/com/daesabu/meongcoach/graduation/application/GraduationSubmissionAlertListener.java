package com.daesabu.meongcoach.graduation.application;

import com.daesabu.meongcoach.alert.application.provided.Alerter;
import com.daesabu.meongcoach.alert.application.provided.dto.AlertRequest;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class GraduationSubmissionAlertListener {

	private final Alerter alerter;

	@TransactionalEventListener
	public void alert(GraduationSubmittedEvent event) {
		alerter.alert(AlertRequest.of("실습 인증 제출",
				AlertField.of("submissionId", event.submissionId()),
				AlertField.of("userId", event.userId()),
				AlertField.of("topicId", event.topicId())));
	}
}
