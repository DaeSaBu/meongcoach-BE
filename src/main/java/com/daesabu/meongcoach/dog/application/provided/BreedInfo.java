package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.Breed;

/**
 * 다른 모듈에 공개하는 강아지 견종 정보.
 */
public record BreedInfo(String code, String label) {

	public static BreedInfo from(Breed breed) {
		return new BreedInfo(breed.name(), breed.getLabel());
	}
}
