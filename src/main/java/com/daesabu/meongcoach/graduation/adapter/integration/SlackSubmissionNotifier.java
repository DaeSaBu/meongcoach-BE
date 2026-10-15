package com.daesabu.meongcoach.graduation.adapter.integration;

import com.daesabu.meongcoach.graduation.adapter.integration.dto.SlackMessage;
import com.daesabu.meongcoach.graduation.application.required.SubmissionNotice;
import com.daesabu.meongcoach.graduation.application.required.SubmissionNotifier;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Slack incoming webhook으로 실습 인증 제출을 운영자 채널에 알린다.
 * 알림은 검수를 돕는 부가 기능이라 전송에 실패해도 제출을 실패시키지 않도록 예외를 삼키고 경고 로그만 남긴다.
 */
@Slf4j
@Component
public class SlackSubmissionNotifier implements SubmissionNotifier {

	private static final DateTimeFormatter SUBMITTED_AT_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneId.of("Asia/Seoul"));
	private static final String MESSAGE_FORMAT =
			"[실습 인증 제출] 토픽 #%d %s · userId %d · submissionId %d · %d번째 제출 · %s";

	private final GraduationSlackProperties properties;
	private final RestClient restClient;

	public SlackSubmissionNotifier(GraduationSlackProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public void notifySubmitted(SubmissionNotice notice) {
		if (!properties.isEnabled()) {
			log.debug("Slack 웹훅이 설정되지 않아 실습 인증 제출 알림을 보내지 않습니다: submissionId={}", notice.submissionId());
			return;
		}
		SlackMessage message = new SlackMessage(MESSAGE_FORMAT.formatted(notice.topicId(), notice.topicTitle(),
				notice.userId(), notice.submissionId(), notice.attemptNo(), SUBMITTED_AT_FORMAT.format(notice.submittedAt())));
		try {
			restClient.post()
					.uri(properties.webhookUrl())
					.contentType(MediaType.APPLICATION_JSON)
					.body(message)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			log.warn("실습 인증 제출 알림을 Slack에 보내지 못했습니다: submissionId={}", notice.submissionId(), e);
		}
	}
}
