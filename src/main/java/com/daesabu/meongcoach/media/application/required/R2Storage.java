package com.daesabu.meongcoach.media.application.required;

/**
 * Cloudflare R2 버킷.
 */
public interface R2Storage extends ObjectStorage {

	/**
	 * 객체 키 앞에 붙는 R2 공개 경로 접두사를 반환한다.
	 */
	String resolvePublicUrlPrefix();
}
