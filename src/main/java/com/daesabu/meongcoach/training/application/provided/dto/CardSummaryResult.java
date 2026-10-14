package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Card;

public record CardSummaryResult(Long id, String title) {

	public static CardSummaryResult from(Card card) {
		return new CardSummaryResult(card.getId(), card.getTitle());
	}
}
