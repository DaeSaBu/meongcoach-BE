package com.daesabu.meongcoach.media.application.provided;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;

/**
 * 클라이언트가 스토리지에 직접 업로드할 수 있는 URL 발급 능력.
 */
public interface MediaUploadUrlIssuer {

	/**
	 * 범위의 사용자 경로 아래에 새 객체 키를 만들고, 범위의 공개 여부에 맞는 스토리지의 업로드 URL과 공개 URL을 발급한다.
	 * Content-Type을 서명에 포함하며, 상한이 있는 요청은 파일 크기도 서명에 포함한다.
	 * 범위의 미디어 종류가 지원하지 않는 형식이면 UnsupportedImageTypeException 또는 UnsupportedVideoTypeException을,
	 * 파일 크기가 없거나 0 이하면 InvalidVideoFileSizeException을, 상한을 넘으면 VideoFileSizeExceededException을 던진다.
	 */
	MediaUploadUrlResult issue(MediaUploadRequest request);
}
