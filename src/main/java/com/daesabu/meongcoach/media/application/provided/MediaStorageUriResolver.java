package com.daesabu.meongcoach.media.application.provided;

import com.daesabu.meongcoach.media.domain.shared.MediaScope;

/**
 * AI 분석기가 직접 읽을 수 있는 분석 스토리지 객체의 URI 조회 능력.
 */
public interface MediaStorageUriResolver {

	/**
	 * 분석 범위 아래의 객체 키면 스토리지 URI(gs://버킷/키)를 반환한다.
	 * 범위 아래의 객체 키가 아니면 InvalidMediaObjectKeyException을 던진다.
	 * 범위가 분석 공개 범위가 아니면 IllegalArgumentException을 던진다.
	 */
	String resolve(MediaScope scope, String objectKey);
}
