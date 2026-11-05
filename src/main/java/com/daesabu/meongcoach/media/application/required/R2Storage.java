package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.MediaObjectKey;

/**
 * Cloudflare R2 버킷.
 */
public interface R2Storage extends ObjectStorage {

	/**
	 * URL이 R2 공개 경로 아래면 가리키는 객체 키를 반환한다.
	 * R2 공개 경로 아래가 아니면 InvalidMediaUrlException을 던진다.
	 */
	MediaObjectKey parsePublicUrl(String url);
}
