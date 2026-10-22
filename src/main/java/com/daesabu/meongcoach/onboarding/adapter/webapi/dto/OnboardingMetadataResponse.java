package com.daesabu.meongcoach.onboarding.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataResult;
import java.util.List;

public record OnboardingMetadataResponse(List<BreedResponse> breeds,
                                         List<PersonalityResponse> personalities) {

	public static OnboardingMetadataResponse from(OnboardingMetadataResult result) {
		return new OnboardingMetadataResponse(
				result.breeds().stream().map(BreedResponse::from).toList(),
				result.personalities().stream().map(PersonalityResponse::from).toList());
	}

	public record BreedResponse(String code, String label, List<String> aliases) {

		public static BreedResponse from(RegisteredBreed breed) {
			return new BreedResponse(breed.name(), breed.getLabel(), breed.getAliases());
		}
	}

	public record PersonalityResponse(String code, String label) {

		public static PersonalityResponse from(Personality personality) {
			return new PersonalityResponse(personality.name(), personality.getLabel());
		}
	}
}
