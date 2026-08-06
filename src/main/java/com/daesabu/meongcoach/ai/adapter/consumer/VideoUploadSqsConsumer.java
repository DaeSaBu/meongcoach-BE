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
 * MVP라 재시도 로직을 두지 않으므로 <b>어떤 예외도 리스너 밖으로 내보내지 않는다.</b>
 * 재전달로 풀릴 수 있는 실패(Gemini 장애, DB 장애)까지 삼키는 대신, 그 경우는 error 로그를 남겨 추적할 수 있게 한다.
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
		// 레코드 단위 처리는 handle이 이미 다 삼키지만, 그 바깥에서 터지는 예외까지 막는 최종 방어선이다
		try {
			dispatch(message);
		} catch (Exception e) {
			log.error("SQS 메시지 처리에 실패했지만 무한 재전달을 막기 위해 메시지를 버린다", e);
		}
	}

	private void dispatch(String message) {
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
		// 레코드 하나가 실패해도 같은 메시지의 나머지 레코드는 계속 처리한다
		for (S3EventMessage.EventRecord record : event.records()) {
			handle(record);
		}
	}

	private void handle(S3EventMessage.EventRecord record) {
		if (record.eventName() == null || !record.eventName().startsWith(OBJECT_CREATED_PREFIX)) {
			log.info("업로드 완료가 아닌 S3 이벤트를 무시한다: {}", record.eventName());
			return;
		}
		String objectKey = objectKeyOf(record);
		if (objectKey == null) {
			return;
		}
		try {
			aiReportGenerator.generate(objectKey);
		} catch (DomainException e) {
			// 키 형식 위반 등 도메인 검증 실패는 재시도해도 같은 결과라 버린다
			log.warn("처리할 수 없는 S3 객체 키라 리포트 생성을 건너뛴다: {}", objectKey, e);
		} catch (Exception e) {
			// Gemini·DB 장애처럼 재전달로 풀릴 수 있는 실패지만, MVP라 재시도를 두지 않고 버린다
			log.error("리포트 생성에 실패했지만 SQS 무한 재전달을 막기 위해 메시지를 버린다: {}", objectKey, e);
		}
	}

	/**
	 * 이벤트에서 객체 키를 꺼내 디코딩한다. 키가 없거나 디코딩할 수 없으면 로그를 남기고 null을 반환한다.
	 * 두 경우 모두 재시도해도 같은 결과라 버리는 쪽이 맞다.
	 */
	private String objectKeyOf(S3EventMessage.EventRecord record) {
		// 알 수 없는 필드를 무시하는 설정이라 s3·object·key가 통째로 빠진 메시지도 그대로 통과한다
		if (record.s3() == null || record.s3().object() == null || record.s3().object().key() == null) {
			log.warn("객체 키가 없는 S3 이벤트를 버린다: {}", record.eventName());
			return null;
		}
		String rawKey = record.s3().object().key();
		try {
			// S3 이벤트의 객체 키는 URL 인코딩되어 온다 (공백은 +)
			return URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			log.warn("URL 디코딩할 수 없는 객체 키를 버린다: {}", rawKey, e);
			return null;
		}
	}
}
