package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.VideoObjectKey;

/**
 * 공개 URL로 제공하는 영상 스토리지 연동 지점.
 */
public interface PublicVideoStorage {

	/**
	 * 주어진 객체 키에 영상을 PUT할 수 있는 업로드 URL과 업로드 후의 공개 URL을 발급한다.
	 * Content-Type과 Content-Length를 서명에 포함하므로 클라이언트는 같은 형식·바이트 수로 업로드해야 한다.
	 */
	VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength);
}
