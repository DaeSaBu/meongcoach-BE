package com.daesabu.meongcoach.media.domain.vo;

import com.daesabu.meongcoach.media.domain.exception.VideoSizeExceededException;

/**
 * 영상 파일 크기 값 객체. 100MB 상한 정책을 이 타입이 소유해 URL 발급 경로와 업로드 완료 확인 경로가 같은 기준을 쓴다.
 * 인스턴스는 of로만 만든다.
 */
public record VideoFileSize(long bytes) {

	/**
	 * 단일 PUT으로 올릴 수 있는 최대 크기(100MB).
	 */
	public static final long MAX_BYTES = 104_857_600L;

	// 정규 생성자에서 검증해 of를 거치지 않고 만든 인스턴스도 상한을 벗어날 수 없게 한다
	public VideoFileSize {
		if (bytes <= 0 || bytes > MAX_BYTES) {
			throw new VideoSizeExceededException(bytes);
		}
	}

	/**
	 * 바이트 수를 검증하며 값 객체로 만든다. 0 이하이거나 상한을 넘으면 예외를 던진다.
	 */
	public static VideoFileSize of(long bytes) {
		return new VideoFileSize(bytes);
	}
}
