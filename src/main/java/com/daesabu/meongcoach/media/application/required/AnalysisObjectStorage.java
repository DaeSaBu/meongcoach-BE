package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.MediaObjectKey;

/**
 * 앱은 유효 시간이 있는 URL로만 쓰고, AI 분석기는 스토리지 URI로 직접 읽는 객체 스토리지.
 */
public interface AnalysisObjectStorage {

	/**
	 * 객체 키에 PUT할 수 있는 업로드 URL과 버킷 표준 도메인 기준의 URL을 발급한다.
	 * contentLength가 있으면 Content-Type과 함께 서명에 포함하고, 없으면 Content-Type만 서명한다.
	 */
	StorageUploadUrl issueUploadUrl(MediaObjectKey key, String contentType, Long contentLength);

	/**
	 * 객체 키를 GET할 수 있는 다운로드 URL을 발급한다.
	 */
	String issueDownloadUrl(MediaObjectKey key);

	/**
	 * AI 분석기가 객체를 직접 읽을 때 쓰는 스토리지 URI(gs://버킷/키)를 반환한다.
	 */
	String storageUri(MediaObjectKey key);
}
