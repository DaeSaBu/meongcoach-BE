package com.daesabu.meongcoach.training.domain;

import org.springframework.test.util.ReflectionTestUtils;

public final class CardMediaFixture {

	private CardMediaFixture() {
	}

	public static CardMedia create(Card card, MediaType mediaType, String url, int sortOrder) {
		CardMedia cardMedia = new CardMedia();
		ReflectionTestUtils.setField(cardMedia, "card", card);
		ReflectionTestUtils.setField(cardMedia, "mediaType", mediaType);
		ReflectionTestUtils.setField(cardMedia, "url", url);
		ReflectionTestUtils.setField(cardMedia, "sortOrder", sortOrder);
		return cardMedia;
	}
}
