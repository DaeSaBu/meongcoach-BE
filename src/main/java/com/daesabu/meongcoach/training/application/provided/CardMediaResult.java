package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.MediaType;

/**
 * 카드 미디어 조회 결과.
 */
public record CardMediaResult(Long id, Long cardId, MediaType mediaType, String url, int sortOrder) {
}
