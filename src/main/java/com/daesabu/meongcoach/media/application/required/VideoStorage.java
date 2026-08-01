package com.daesabu.meongcoach.media.application.required;

import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.util.Optional;

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

	/**
	 * 주어진 객체 키에 실제로 저장된 영상을 조회한다. 업로드 완료 여부를 클라이언트 신고가 아니라 스토리지에 직접 묻는 통로다.
	 * 저장된 객체가 없으면 빈 값을 돌려준다.
	 */
	Optional<StoredVideo> findStoredVideo(VideoObjectKey key);
}
