package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.Card;
import java.util.List;

public record CardListResponse(List<CardResponse> cards) {

	public static CardListResponse from(List<Card> cards) {
		List<CardResponse> cardResponses = cards.stream()
				.map(CardResponse::from)
				.toList();
		return new CardListResponse(cardResponses);
	}
}
