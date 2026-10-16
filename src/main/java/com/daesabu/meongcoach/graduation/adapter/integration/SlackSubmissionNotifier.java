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

@Slf4j
@Component
public class SlackSubmissionNotifier implements SubmissionNotifier {

	private static final DateTimeFormatter SUBMITTED_AT_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneId.of("Asia/Seoul"));
	private static final String MESSAGE_FORMAT =
			"[실습 인증 제출] 토픽 #%d %s · userId %d · graduationId %d · %d번째 제출 · %s\n영상(1시간): %s\n키: %s";

	private final GraduationSlackProperties properties;
	private final RestClient restClient;

	public SlackSubmissionNotifier(GraduationSlackProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public void notifySubmitted(SubmissionNotice notice) {
		if (!properties.isEnabled()) {
			log.debug("Slack 웹훅이 설정되지 않아 실습 인증 제출 알림을 보내지 않습니다: graduationId={}", notice.graduationId());
			return;
		}
		SlackMessage message = new SlackMessage(MESSAGE_FORMAT.formatted(notice.topicId(), notice.topicTitle(),
				notice.userId(), notice.graduationId(), notice.attemptNo(), SUBMITTED_AT_FORMAT.format(notice.submittedAt()),
				notice.videoUrl(), notice.videoObjectKey()));
		try {
			restClient.post()
					.uri(properties.webhookUrl())
					.contentType(MediaType.APPLICATION_JSON)
					.body(message)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			log.warn("실습 인증 제출 알림을 Slack에 보내지 못했습니다: graduationId={}", notice.graduationId(), e);
		}
	}
}
