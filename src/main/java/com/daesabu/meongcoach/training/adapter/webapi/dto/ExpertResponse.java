package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.Expert;

public record ExpertResponse(Long expertId, String name, String title, String description, String imageUrl) {

	public static ExpertResponse from(Expert expert) {
		return new ExpertResponse(expert.getId(), expert.getName(), expert.getTitle(), expert.getDescription(),
				expert.getImageUrl());
	}
}
