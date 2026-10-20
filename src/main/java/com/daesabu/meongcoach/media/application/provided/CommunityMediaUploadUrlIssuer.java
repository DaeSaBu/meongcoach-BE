package com.daesabu.meongcoach.media.application.provided;

/**
 * 커뮤니티 게시글에 첨부할 사진·영상의 업로드 URL 발급 능력.
 */
public interface CommunityMediaUploadUrlIssuer {

	/**
	 * 사용자의 커뮤니티 이미지 업로드 URL을 발급한다.
	 * 지원하지 않는 형식이면 UnsupportedImageTypeException을 던진다.
	 */
	CommunityMediaUploadUrlResult issueImage(Long userId, String contentType);

	/**
	 * 사용자의 커뮤니티 영상 업로드 URL을 발급하고, 파일 크기를 업로드 URL 서명에 포함한다.
	 * 지원하지 않는 형식이면 UnsupportedVideoTypeException, 크기가 0 이하면 InvalidVideoFileSizeException,
	 * 512MiB를 넘으면 VideoFileSizeExceededException을 던진다.
	 */
	CommunityMediaUploadUrlResult issueVideo(Long userId, String contentType, long fileSizeBytes);
}
