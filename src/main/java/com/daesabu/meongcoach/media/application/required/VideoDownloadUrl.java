package com.daesabu.meongcoach.media.application.required;

/**
 * 스토리지가 발급한 영상 다운로드 URL. 비공개 버킷의 객체를 읽을 수 있는 presigned URL과 공개 URL을 함께 담는다.
 * s3Uri는 AWS 서비스가 버킷에서 객체를 직접 읽을 때 쓰는 s3://버킷/키 형식의 주소다.
 */
public record VideoDownloadUrl(String downloadUrl, String publicUrl, String s3Uri, long expiresInSeconds) {
}
