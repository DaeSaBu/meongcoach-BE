package com.daesabu.meongcoach.ai.domain;

import java.time.LocalDateTime;

/**
 * 업로드 URL 발급 시 UPLOADING 리포트를 만들기 위한 입력.
 *
 * @param userId          영상 소유자
 * @param videoObjectKey  발급한 영상 객체 키
 * @param uploadExpiresAt 업로드 URL 만료 시각. 이 시각까지 업로드 완료 이벤트가 오지 않으면 FAILED_UPLOAD로 본다
 */
public record AiReportUploadCommand(Long userId, String videoObjectKey, LocalDateTime uploadExpiresAt) {
}
