package com.daesabu.meongcoach.media.application.provided;

/**
 * 영상 업로드 URL 발급 결과.
 *
 * @param uploadUrl        클라이언트가 영상을 PUT할 presigned URL. Content-Type과 Content-Length가 서명에 포함되므로
 *                         발급 요청에 쓴 값과 정확히 같게 보내야 한다
 * @param publicUrl        업로드 완료 후 영상이 공개되는 URL
 * @param objectKey        발급된 객체 키. 업로드를 마친 뒤 완료 확인 API에 이 값을 그대로 돌려준다
 * @param expiresInSeconds uploadUrl의 유효 시간(초)
 */
public record VideoUploadUrlResult(String uploadUrl, String publicUrl, String objectKey, long expiresInSeconds) {
}
