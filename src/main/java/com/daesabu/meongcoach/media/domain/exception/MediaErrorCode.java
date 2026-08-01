package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum MediaErrorCode implements ErrorCode {

	MEDIA_INVALID_UPLOAD_TARGET(400, "업로드 대상이 올바르지 않습니다."),
	MEDIA_UNSUPPORTED_IMAGE_TYPE(400, "지원하지 않는 이미지 형식입니다."),
	MEDIA_INVALID_IMAGE_URL(400, "이미지 URL이 올바르지 않습니다."),
	MEDIA_UNSUPPORTED_VIDEO_TYPE(400, "지원하지 않는 영상 형식입니다."),
	MEDIA_VIDEO_SIZE_EXCEEDED(400, "영상 크기가 허용 범위를 벗어났습니다."),
	MEDIA_INVALID_OBJECT_KEY(400, "영상 객체 키가 올바르지 않습니다."),
	MEDIA_VIDEO_ACCESS_DENIED(403, "해당 영상에 접근할 권한이 없습니다."),
	MEDIA_VIDEO_NOT_UPLOADED(404, "업로드되지 않은 영상입니다."),
	;

	private final int status;
	private final String message;

	MediaErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public String message() {
		return message;
	}

	@Override
	public int status() {
		return status;
	}
}
