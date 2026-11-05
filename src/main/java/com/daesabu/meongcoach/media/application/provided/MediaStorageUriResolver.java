package com.daesabu.meongcoach.media.application.provided;

import com.daesabu.meongcoach.media.domain.shared.MediaScope;

/**
 * GCS 객체의 스토리지 URI 조회 능력.
 */
public interface MediaStorageUriResolver {

	/**
	 * 범위 아래의 객체 키면 스토리지 URI(gs://버킷/키)를 반환한다.
	 * 범위 아래의 객체 키가 아니면 InvalidMediaObjectKeyException을 던진다.
	 */
	String resolve(MediaScope scope, String objectKey);
}
