package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CardMediaView;
import com.daesabu.meongcoach.training.domain.MediaType;

/**
 * 카드 미디어 응답.
 */
public record CardMediaResponse(Long cardMediaId, Long cardId, MediaType mediaType, String url, int sortOrder) {

	public static CardMediaResponse from(CardMediaView view) {
		return new CardMediaResponse(view.id(), view.cardId(), view.mediaType(), view.url(), view.sortOrder());
	}
}
