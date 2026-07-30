package com.daesabu.meongcoach.media.domain;

/**
 * 업로드를 허용하는 이미지 형식. Content-Type과 객체 키 확장자의 매핑을 가진다.
 */
public enum ImageType {
	JPEG("image/jpeg", "jpg"),
	PNG("image/png", "png"),
	WEBP("image/webp", "webp"),
	;

	private final String contentType;
	private final String extension;

	ImageType(String contentType, String extension) {
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
	public static ImageType fromContentType(String contentType) {
		throw new UnsupportedOperationException("아직 구현되지 않았다");
	}
}
