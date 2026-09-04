package com.daesabu.meongcoach.ai.application.provided;

/**
 * AI 리포트용 영상 업로드 URL 발급 결과.
 *
 * @param reportId         발급과 함께 UPLOADING으로 만든 리포트 ID. 잃어버려도 목록 조회로 다시 찾을 수 있다
 * @param uploadUrl        클라이언트가 영상을 PUT할 presigned URL. 발급 시 지정한 Content-Type과 Content-Length로 보내야 한다
 * @param publicUrl        버킷 공개 도메인 기준의 영상 URL. 버킷을 비공개로 운영하면 직접 접근은 거부된다
 * @param objectKey        업로드된 객체의 키
 * @param expiresInSeconds uploadUrl의 유효 시간(초). 이 안에 업로드 완료 이벤트가 오지 않으면 리포트는 FAILED_UPLOAD로 조회된다
 */
public record AiVideoUploadUrlResult(Long reportId, String uploadUrl, String publicUrl, String objectKey,
		long expiresInSeconds) {
}
