package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import java.util.Optional;

/**
 * 공개 URL로 제공하는 객체 스토리지.
 */
public interface PublicObjectStorage {

	/**
	 * 객체 키에 PUT할 수 있는 업로드 URL과 업로드 후의 공개 URL을 발급한다.
	 * contentLength가 있으면 Content-Type과 함께 서명에 포함하고, 없으면 Content-Type만 서명한다.
	 */
	StorageUploadUrl issueUploadUrl(MediaObjectKey key, String contentType, Long contentLength);

	/**
	 * URL이 이 스토리지의 공개 URL이면 가리키는 객체 키를 반환한다.
	 */
	Optional<MediaObjectKey> objectKeyOf(String url);
}
