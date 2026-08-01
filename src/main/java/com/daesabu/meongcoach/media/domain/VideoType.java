package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import java.util.Arrays;

/**
 * 업로드를 허용하는 영상 형식. Content-Type과 객체 키 확장자의 매핑을 가진다.
 */
public enum VideoType {
	MP4("video/mp4", "mp4"),
	QUICKTIME("video/quicktime", "mov"),
	;

	private final String contentType;
	private final String extension;

	VideoType(String contentType, String extension) {
		this.contentType = contentType;
		this.extension = extension;
	}

	public String getContentType() {
		return contentType;
	}

	public String getExtension() {
		return extension;
	}

	/**
	 * Content-Type 문자열을 enum으로 변환한다. 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
	public static VideoType fromContentType(String contentType) {
		return Arrays.stream(values())
				.filter(type -> type.contentType.equals(contentType))
				.findFirst()
				.orElseThrow(() -> new UnsupportedVideoTypeException(contentType));
	}

	/**
	 * 객체 키의 확장자를 enum으로 변환한다. 객체 키를 파싱할 때 마지막 조각의 확장자가 허용 목록에 있는지 확인하는 데 쓴다.
	 */
	public static VideoType fromExtension(String extension) {
		return Arrays.stream(values())
				.filter(type -> type.extension.equals(extension))
				.findFirst()
				.orElseThrow(() -> new UnsupportedVideoTypeException(extension));
	}
}
