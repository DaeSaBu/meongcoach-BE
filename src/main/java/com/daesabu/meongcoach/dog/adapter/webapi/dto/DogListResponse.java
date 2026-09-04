package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.domain.Dog;
import java.util.List;

/**
 * 보유 강아지 목록 조회 응답.
 */
public record DogListResponse(List<DogResponse> dogs) {

	public static DogListResponse from(List<Dog> dogs) {
		List<DogResponse> responses = dogs.stream()
				.map(DogResponse::from)
				.toList();
		return new DogListResponse(responses);
	}
}
