package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;

/**
 * 영상 스토리지 연동 지점. 구현은 adapter/client의 S3 어댑터가 담당한다.
 */
public interface VideoStorage {

	/**
	 * 주어진 객체 키에 영상을 PUT할 수 있는 업로드 URL을 발급한다.
	 * Content-Length까지 서명에 포함하므로 클라이언트는 정확히 이 바이트 수로 업로드해야 한다.
	 */
	VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength);
}
