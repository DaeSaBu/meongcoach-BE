package com.daesabu.meongcoach.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.application.BreedFinderService;
import com.daesabu.meongcoach.dog.application.PersonalityFinderService;
import com.daesabu.meongcoach.dog.application.provided.BreedInfo;
import com.daesabu.meongcoach.dog.application.provided.PersonalityInfo;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataResult;
import com.daesabu.meongcoach.training.application.TopicFinderService;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.user.application.MbtiFinderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class OnboardingMetadataServiceTest {

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private TestEntityManager entityManager;

	private OnboardingMetadataService service;

	@BeforeEach
	void setUp() {
		service = new OnboardingMetadataService(
				new TopicFinderService(topicRepository),
				new BreedFinderService(),
				new PersonalityFinderService(),
				new MbtiFinderService());
	}

	@Test
	void 토픽_견종_성격_MBTI_목록을_한_번에_모아_반환한다() {
		TrainingCategory category = entityManager.persist(TrainingCategory.create("기본 훈련", 1, null, null));
		entityManager.persist(Topic.create(category, new TopicCreateCommand("산책 훈련", 2, null, null, null)));
		entityManager.persist(Topic.create(category, new TopicCreateCommand("배변 훈련", 1, null, null, null)));

		OnboardingMetadataResult result = service.find();

		assertThat(result.topics()).extracting(TopicSummary::title)
				.containsExactly("배변 훈련", "산책 훈련");
		assertThat(result.breeds()).extracting(BreedInfo::code)
				.hasSize(31)
				.startsWith("MALTESE")
				.endsWith("MIXED");
		assertThat(result.personalities()).extracting(PersonalityInfo::code)
				.containsExactly("TIMID", "LIVELY", "FRIENDLY", "CALM", "FEARFUL", "AFFECTIONATE",
						"INDEPENDENT", "PLAYFUL", "EXCITABLE", "STUBBORN");
		assertThat(result.mbtis()).hasSize(16).contains("ISTJ", "ENFP");
	}

	@Test
	void 토픽이_없어도_견종_성격_MBTI_목록은_그대로_반환한다() {
		OnboardingMetadataResult result = service.find();

		assertThat(result.topics()).isEmpty();
		assertThat(result.breeds()).isNotEmpty();
		assertThat(result.personalities()).isNotEmpty();
		assertThat(result.mbtis()).isNotEmpty();
	}
}
