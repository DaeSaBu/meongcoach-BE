package com.daesabu.meongcoach.media.application.provided;

/**
 * 업로드 완료 확인 결과. 여기 담긴 값은 클라이언트 신고값이 아니라 스토리지가 보고한 실제 값이다.
 *
 * @param objectKey   확인이 끝난 객체 키
 * @param publicUrl   업로드된 영상이 공개되는 URL. 후속 API에는 이 값을 그대로 넘긴다
 * @param contentType 스토리지가 보관 중인 객체의 Content-Type
 * @param sizeBytes   스토리지가 보고한 실제 객체 크기(바이트)
 */
public record VerifiedVideoResult(String objectKey, String publicUrl, String contentType, long sizeBytes) {
}
