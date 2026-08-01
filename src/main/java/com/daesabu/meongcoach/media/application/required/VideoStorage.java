package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;

/**
 * 영상 스토리지 연동 지점. 구현은 adapter/client의 R2 어댑터가 담당한다.
 * 이미지와 요구 사항(크기 상한, 서명 대상 헤더, 유효 시간)이 달라 ImageStorage와 분리해 둔다.
 */
public interface VideoStorage {

	/**
	 * 주어진 객체 키에 영상을 PUT할 수 있는 업로드 URL을 발급한다.
	 * contentLength까지 서명에 포함하므로 클라이언트는 발급 요청과 정확히 같은 크기로만 업로드할 수 있다.
	 */
	VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength);
}
