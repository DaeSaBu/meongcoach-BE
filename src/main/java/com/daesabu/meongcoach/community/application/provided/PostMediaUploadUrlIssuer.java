package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.PostMediaUploadUrlRequest;
import com.daesabu.meongcoach.media.application.provided.CommunityMediaUploadUrlResult;
import jakarta.validation.Valid;

/**
 * 게시글에 첨부할 사진·영상의 업로드 URL 발급 능력.
 */
public interface PostMediaUploadUrlIssuer {

	/**
	 * 미디어 종류(IMAGE·VIDEO)에 맞는 사용자의 업로드 URL과 업로드 후의 공개 URL을 발급한다.
	 * 영상은 파일 크기를 업로드 URL 서명에 포함하고 512MiB를 넘으면 VideoFileSizeExceededException을 던진다.
	 * 지원하지 않는 미디어 종류면 InvalidPostMediaTypeException, 종류에 맞지 않는 형식이면
	 * UnsupportedImageTypeException 또는 UnsupportedVideoTypeException을 던진다.
	 */
	CommunityMediaUploadUrlResult issue(Long userId, @Valid PostMediaUploadUrlRequest request);
}
