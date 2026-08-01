package com.daesabu.meongcoach.media.application.provided;

/**
 * 클라이언트가 보낸 이미지 URL이 우리 스토리지의 공개 URL인지 검증한다.
 * 업로드 완료 알림 없이 클라이언트 확인 방식을 쓰므로, URL 등록 시점에 이 검증으로 외부 URL 주입을 막는다.
 */
public interface StoredImageUrlValidator {

	/**
	 * 우리 스토리지 URL이 아니면 {@code InvalidImageUrlException}을 던진다.
	 * null·빈 문자열은 이미지 미설정이므로 통과시킨다.
	 */
	void validate(String url);
}
