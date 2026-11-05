package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.MediaObjectKey;

/**
 * Google Cloud Storage 버킷.
 */
public interface GcsStorage extends ObjectStorage {

	/**
	 * 객체 키를 GET할 수 있는 다운로드 URL을 발급한다.
	 */
	String issueDownloadUrl(MediaObjectKey key);

	/**
	 * 객체의 스토리지 URI(gs://버킷/키)를 반환한다.
	 */
	String storageUri(MediaObjectKey key);
}
