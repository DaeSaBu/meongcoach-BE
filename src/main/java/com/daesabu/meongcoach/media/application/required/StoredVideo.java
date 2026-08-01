package com.daesabu.meongcoach.media.application.required;

/**
 * 스토리지에 실제로 저장돼 있는 영상의 정보. 클라이언트가 신고한 값이 아니라 스토리지가 보고한 값이다.
 *
 * @param contentType 스토리지가 보관 중인 객체의 Content-Type
 * @param sizeBytes   스토리지가 보고한 실제 객체 크기(바이트)
 */
public record StoredVideo(String contentType, long sizeBytes) {
}
