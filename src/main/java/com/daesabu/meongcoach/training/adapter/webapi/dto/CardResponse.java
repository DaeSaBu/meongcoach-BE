package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CardView;
import java.util.List;

/**
 * 카드 응답. 소속 미디어를 함께 담는다.
 */
public record CardResponse(Long cardId, int cardSortOrder, String instruction, List<CardMediaResponse> cardMedia) {

	public static CardResponse from(CardView view) {
		List<CardMediaResponse> cardMedia = view.cardMedia().stream()
				.map(CardMediaResponse::from)
				.toList();
		return new CardResponse(view.id(), view.sortOrder(), view.instruction(), cardMedia);
	}
}
