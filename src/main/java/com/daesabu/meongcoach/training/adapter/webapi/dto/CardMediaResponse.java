package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.CardMedia;
import com.daesabu.meongcoach.training.domain.MediaType;

public record CardMediaResponse(Long cardMediaId, Long cardId, MediaType mediaType, String url) {

	public static CardMediaResponse from(CardMedia cardMedia) {
		return new CardMediaResponse(cardMedia.getId(), cardMedia.getCard().getId(), cardMedia.getMediaType(),
				cardMedia.getUrl());
	}
}
