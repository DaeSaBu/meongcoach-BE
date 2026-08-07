package com.daesabu.meongcoach.media.application.provided;

/**
 * 영상 다운로드 URL 발급 결과.
 *
 * @param downloadUrl      영상을 GET할 수 있는 presigned URL. 비공개 버킷에서도 유효 시간 동안 접근할 수 있다
 * @param publicUrl        버킷 공개 도메인 기준의 영상 URL. 만료가 없다
 * @param s3Uri            s3://버킷/키 형식의 주소. AWS 서비스가 버킷에서 객체를 직접 읽을 때 쓴다
 * @param ownerUserId      객체 키 경로에서 추출한 업로더의 사용자 ID
 * @param expiresInSeconds downloadUrl의 유효 시간(초)
 */
public record VideoDownloadUrlResult(String downloadUrl, String publicUrl, String s3Uri, Long ownerUserId,
		long expiresInSeconds) {
}
