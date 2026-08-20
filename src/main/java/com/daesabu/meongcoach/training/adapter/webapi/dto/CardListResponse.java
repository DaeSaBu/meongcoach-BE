package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.CardResult;
import java.util.List;

/**
 * 레슨 시작 응답. 레슨의 카드 전체를 담는다.
 */
public record CardListResponse(List<CardResponse> cards) {

	public static CardListResponse from(List<CardResult> results) {
		List<CardResponse> cards = results.stream()
				.map(CardResponse::from)
				.toList();
		return new CardListResponse(cards);
	}
}
