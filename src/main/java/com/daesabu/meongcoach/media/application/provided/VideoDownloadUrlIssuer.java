package com.daesabu.meongcoach.media.application.provided;

/**
 * 업로드된 영상을 읽을 수 있는 다운로드 URL을 발급한다.
 * 모듈 경계를 넘는 값이라 객체 키를 문자열로 받고, 검증·소유자 추출은 media 모듈이 수행한다.
 */
public interface VideoDownloadUrlIssuer {

	VideoDownloadUrlResult issue(String objectKey);
}
