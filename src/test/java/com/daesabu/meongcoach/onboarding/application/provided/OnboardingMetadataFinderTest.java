package com.daesabu.meongcoach.onboarding.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.TopicsResult;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class OnboardingMetadataFinderTest {

	@Autowired
	private OnboardingMetadataFinder onboardingMetadataFinder;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 토픽_견종_성격_MBTI_목록을_한_번에_모아_반환한다() {
		TrainingCategory category = trainingCategoryRepository.save(
				TrainingCategoryFixture.create("기본 훈련", 1, null, null));
		topicRepository.save(TopicFixture.create(category, "산책 훈련", 2, null, null, null));
		topicRepository.save(TopicFixture.create(category, "배변 훈련", 1, null, null, null));
		entityManager.flush();
		entityManager.clear();

		OnboardingMetadataResult result = onboardingMetadataFinder.find();

		assertThat(result.topics()).extracting(TopicsResult::title)
				.containsExactly("배변 훈련", "산책 훈련");
		assertThat(result.breeds()).extracting(Breed::name)
				.hasSize(31)
				.startsWith("MIXED")
				.endsWith("FRENCH_BULLDOG");
		assertThat(result.personalities()).extracting(Personality::name)
				.containsExactly("TIMID", "LIVELY", "FRIENDLY", "CALM", "FEARFUL", "AFFECTIONATE",
						"INDEPENDENT", "PLAYFUL", "EXCITABLE", "STUBBORN");
		assertThat(result.mbtis()).hasSize(16).contains("ISTJ", "ENFP");
	}

	@Test
	void 토픽이_없어도_견종_성격_MBTI_목록은_그대로_반환한다() {
		OnboardingMetadataResult result = onboardingMetadataFinder.find();

		assertThat(result.topics()).isEmpty();
		assertThat(result.breeds()).isNotEmpty();
		assertThat(result.personalities()).isNotEmpty();
		assertThat(result.mbtis()).isNotEmpty();
	}
}
