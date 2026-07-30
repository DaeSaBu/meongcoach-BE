package com.daesabu.meongcoach.media.application.required;

/**
 * 이미지 스토리지 연동 지점. 구현은 adapter/client의 R2 어댑터가 담당한다.
 */
public interface ImageStorage {

	/**
	 * 주어진 객체 키에 이미지를 PUT할 수 있는 업로드 URL을 발급한다.
	 */
	ImageUploadUrl issueUploadUrl(String key, String contentType);

	/**
	 * URL이 이 스토리지의 공개 URL인지 판별한다.
	 */
	boolean isPublicUrl(String url);
}
