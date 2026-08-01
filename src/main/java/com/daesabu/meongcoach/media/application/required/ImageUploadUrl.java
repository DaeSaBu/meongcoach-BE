package com.daesabu.meongcoach.media.application.required;

/**
 * 스토리지가 발급한 업로드 URL. 업로드용 presigned URL과 업로드 완료 후의 공개 URL을 함께 담는다.
 */
public record ImageUploadUrl(String uploadUrl, String publicUrl, long expiresInSeconds) {
}
