package com.daesabu.meongcoach.media.application.provided;

import com.daesabu.meongcoach.media.domain.shared.MediaScope;

/**
 * 클라이언트가 보낸 미디어 URL·객체 키가 범위와 사용자에 맞게 우리 스토리지에 올린 것인지 확인한다.
 */
public interface StoredMediaVerifier {

	/**
	 * URL이 공개 스토리지에서 범위의 사용자 경로 바로 아래 파일을 가리키지 않으면 InvalidMediaUrlException을 던진다.
	 * null·빈 문자열은 미디어 미설정으로 보고 통과시킨다.
	 */
	void validateUrl(MediaScope scope, Long userId, String url);

	/**
	 * 객체 키가 범위의 사용자 경로 바로 아래 파일이면 true를 반환한다.
	 */
	boolean isOwnedObjectKey(MediaScope scope, Long userId, String objectKey);
}
