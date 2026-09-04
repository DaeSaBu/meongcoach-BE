package com.daesabu.meongcoach.onboarding.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.application.provided.BreedInfo;
import com.daesabu.meongcoach.dog.application.provided.PersonalityInfo;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataResult;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import java.util.List;

public record OnboardingMetadataResponse(List<TopicResponse> topics, List<BreedResponse> breeds,
                                         List<PersonalityResponse> personalities, List<String> mbtis) {

	public static OnboardingMetadataResponse from(OnboardingMetadataResult result) {
		return new OnboardingMetadataResponse(
				result.topics().stream().map(TopicResponse::from).toList(),
				result.breeds().stream().map(BreedResponse::from).toList(),
				result.personalities().stream().map(PersonalityResponse::from).toList(),
				result.mbtis());
	}

	public record TopicResponse(Long id, String title) {

		public static TopicResponse from(TopicSummary topic) {
			return new TopicResponse(topic.id(), topic.title());
		}
	}

	public record BreedResponse(String code, String label) {

		public static BreedResponse from(BreedInfo breed) {
			return new BreedResponse(breed.code(), breed.label());
		}
	}

	public record PersonalityResponse(String code, String label) {

		public static PersonalityResponse from(PersonalityInfo personality) {
			return new PersonalityResponse(personality.code(), personality.label());
		}
	}
}
