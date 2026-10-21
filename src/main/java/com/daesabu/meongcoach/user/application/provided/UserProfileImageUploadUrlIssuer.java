package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;

/**
 * 사용자 프로필 이미지 업로드 URL 발급 능력.
 */
public interface UserProfileImageUploadUrlIssuer {

	/**
	 * 사용자의 프로필 이미지 경로에 업로드 URL과 업로드 후의 공개 URL을 발급한다.
	 * 지원하지 않는 contentType이면 UnsupportedImageTypeException을 던진다.
	 */
	MediaUploadUrlResult issue(Long userId, String contentType);
}
