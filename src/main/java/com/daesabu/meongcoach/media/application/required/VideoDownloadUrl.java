package com.daesabu.meongcoach.media.application.required;

/**
 * 스토리지가 발급한 영상 다운로드 URL. 비공개 버킷의 객체를 읽을 수 있는 presigned URL과 공개 URL,
 * 키 규칙으로 유도한 썸네일 공개 URL을 함께 담는다.
 */
public record VideoDownloadUrl(String downloadUrl, String publicUrl, String thumbnailUrl, long expiresInSeconds) {
}
