package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.MediaObjectKey;

/**
 * 유효 시간이 있는 URL로만 읽을 수 있는 객체 스토리지.
 */
public interface PrivateObjectStorage {

	/**
	 * 객체 키에 PUT할 수 있는 업로드 URL과 버킷 공개 도메인 기준의 URL을 발급한다.
	 * contentLength가 있으면 Content-Type과 함께 서명에 포함하고, 없으면 Content-Type만 서명한다.
	 */
	StorageUploadUrl issueUploadUrl(MediaObjectKey key, String contentType, Long contentLength);

	/**
	 * 객체 키를 GET할 수 있는 다운로드 URL을 발급한다.
	 */
	String issueDownloadUrl(MediaObjectKey key);
}
