package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.Card;
import java.util.List;

public record CardResponse(Long cardId, String cardTitle, String instruction, List<CardMediaResponse> cardMedia) {

	public static CardResponse from(Card card) {
		List<CardMediaResponse> cardMedia = card.getCardMedia().stream()
				.map(CardMediaResponse::from)
				.toList();
		return new CardResponse(card.getId(), card.getTitle(), card.getInstruction(), cardMedia);
	}
}
