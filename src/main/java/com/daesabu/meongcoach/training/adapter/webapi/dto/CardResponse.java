package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CardResult;
import java.util.List;

/**
 * 카드 응답. 소속 미디어를 함께 담는다.
 */
public record CardResponse(Long cardId, String cardTitle, int cardSortOrder, String instruction,
		List<CardMediaResponse> cardMedia) {

	public static CardResponse from(CardResult result) {
		List<CardMediaResponse> cardMedia = result.cardMedia().stream()
				.map(CardMediaResponse::from)
				.toList();
		return new CardResponse(result.id(), result.title(), result.sortOrder(), result.instruction(), cardMedia);
	}
}
