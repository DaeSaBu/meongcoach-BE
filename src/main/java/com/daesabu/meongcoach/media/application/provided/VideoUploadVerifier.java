package com.daesabu.meongcoach.media.application.provided;

/**
 * 클라이언트가 업로드를 마친 영상이 스토리지에 실제로 저장됐는지 확인한다.
 * 모듈 경계를 넘는 값이라 객체 키를 문자열로 받고, 형식 검증과 소유권 판별은 media 모듈이 수행한다.
 */
public interface VideoUploadVerifier {

	VerifiedVideoResult verify(Long userId, String objectKey);
}
