package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import java.util.Optional;

public record MyAuthorResponse(AuthorResponse author) {

	public static MyAuthorResponse from(Optional<AuthorResult> author) {
		return new MyAuthorResponse(author.map(AuthorResponse::from).orElse(null));
	}
}
