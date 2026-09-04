package com.daesabu.meongcoach.ai.adapter.integration;

/**
 * EvoLink가 HTTP 200으로 응답했지만 내용을 쓸 수 없는 경우(choices 없음, 콘텐츠 검토 차단, 토큰 상한 잘림, 빈 내용).
 * 공용 클라이언트는 영상 분석인지 제목 생성인지 모르므로 도메인 예외로 바로 번역하지 않고, 이 예외로 알리면 각 어댑터가 번역한다.
 */
class EvoLinkResponseException extends RuntimeException {

	EvoLinkResponseException(String message) {
		super(message);
	}
}
