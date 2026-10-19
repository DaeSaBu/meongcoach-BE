package com.daesabu.meongcoach.media.application.provided;

/**
 * 클라이언트가 스토리지에 직접 업로드할 수 있는 영상 업로드 URL을 발급한다.
 * 업로드 대상·영상 형식·파일 크기를 검증한다.
 */
public interface VideoUploadUrlIssuer {

	VideoUploadUrlResult issue(Long userId, String target, String contentType, long fileSizeBytes);
}
