package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.Personality;

/**
 * 다른 모듈에 공개하는 강아지 성격 정보.
 */
public record PersonalityInfo(String code, String label) {

	public static PersonalityInfo from(Personality personality) {
		return new PersonalityInfo(personality.name(), personality.getLabel());
	}
}
