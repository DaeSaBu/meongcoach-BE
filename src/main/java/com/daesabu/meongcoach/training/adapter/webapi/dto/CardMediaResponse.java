package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CardMediaResult;
import com.daesabu.meongcoach.training.domain.MediaType;

/**
 * 카드 미디어 응답.
 */
public record CardMediaResponse(Long cardMediaId, Long cardId, MediaType mediaType, String url, int sortOrder) {

	public static CardMediaResponse from(CardMediaResult result) {
		return new CardMediaResponse(result.id(), result.cardId(), result.mediaType(), result.url(), result.sortOrder());
	}
}
