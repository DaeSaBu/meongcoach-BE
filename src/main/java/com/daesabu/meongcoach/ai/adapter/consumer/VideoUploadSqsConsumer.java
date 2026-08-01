package com.daesabu.meongcoach.ai.adapter.consumer;

import com.daesabu.meongcoach.ai.adapter.consumer.dto.S3EventMessage;
import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.shared.exception.DomainException;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * S3 영상 업로드 완료 이벤트를 SQS로 받아 AI 리포트 생성을 트리거한다.
 * 리스너가 정상 반환하면 메시지가 삭제되고, 예외가 나가면 visibility timeout 후 재전달된다.
 * 재시도해도 소용없는 메시지(형식 위반)는 로그만 남기고 정상 반환해 무한 재시도를 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadSqsConsumer {

	private static final String OBJECT_CREATED_PREFIX = "ObjectCreated";

	private final AiReportGenerator aiReportGenerator;
	private final ObjectMapper objectMapper;

	@SqsListener("${meongcoach.ai.video-queue}")
	public void consume(String message) {
		S3EventMessage event;
		try {
			event = objectMapper.readValue(message, S3EventMessage.class);
		} catch (JacksonException e) {
			// 파싱 불가 메시지는 재시도해도 같은 결과라 버린다
			log.warn("S3 이벤트 형식이 아닌 SQS 메시지를 버린다", e);
			return;
		}
		if (event.records() == null || event.records().isEmpty()) {
			// 이벤트 알림 연결 시 S3가 보내는 s3:TestEvent 등은 Records가 없다
			log.info("Records 없는 SQS 메시지를 무시한다");
			return;
		}
		for (S3EventMessage.EventRecord record : event.records()) {
			handle(record);
		}
	}

	private void handle(S3EventMessage.EventRecord record) {
		if (record.eventName() == null || !record.eventName().startsWith(OBJECT_CREATED_PREFIX)) {
			log.info("업로드 완료가 아닌 S3 이벤트를 무시한다: {}", record.eventName());
			return;
		}
		// S3 이벤트의 객체 키는 URL 인코딩되어 온다 (공백은 +)
		String objectKey = URLDecoder.decode(record.s3().object().key(), StandardCharsets.UTF_8);
		try {
			aiReportGenerator.generate(objectKey);
		} catch (DomainException e) {
			// 키 형식 위반 등 도메인 검증 실패는 재시도해도 같은 결과라 버린다
			log.warn("처리할 수 없는 S3 객체 키라 리포트 생성을 건너뛴다: {}", objectKey, e);
		}
	}
}
