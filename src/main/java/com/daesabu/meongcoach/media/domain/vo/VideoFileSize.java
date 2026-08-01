package com.daesabu.meongcoach.media.domain.vo;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;

/**
 * 영상 파일 크기 값 객체. 생성 시점에 허용 범위를 검증하므로 인스턴스가 존재하면 항상 업로드 가능한 크기다.
 * 이 값은 presigned URL의 Content-Length 서명에 그대로 들어가 실제 업로드 크기를 강제한다.
 */
public record VideoFileSize(long bytes) {

	// 멀티파트 없이 단일 PUT으로 올리는 상한. 환경이 아니라 제품 규칙이라 설정이 아닌 도메인 상수로 둔다
	public static final long MAX_BYTES = 100L * 1024 * 1024;

	public VideoFileSize {
		if (bytes <= 0) {
			throw new InvalidVideoFileSizeException(bytes);
		}
		if (bytes > MAX_BYTES) {
			throw new VideoFileSizeExceededException(bytes, MAX_BYTES);
		}
	}
}
