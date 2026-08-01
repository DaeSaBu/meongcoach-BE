package com.daesabu.meongcoach.media.application.required;

/**
 * 스토리지가 발급한 영상 업로드 URL. 업로드용 presigned URL과 업로드된 객체를 가리키는 값들을 함께 담는다.
 */
public record VideoUploadUrl(String uploadUrl, String publicUrl, String objectKey, long expiresInSeconds) {
}
