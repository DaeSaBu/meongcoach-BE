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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("온보딩 메타데이터 조회 서비스")
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
	@DisplayName("토픽·견종·성격·MBTI 목록을 한 번에 모아 반환한다")
	void findCollectsTopicsBreedsPersonalitiesAndMbtis() {
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
	@DisplayName("토픽이 없어도 견종·성격·MBTI 목록은 그대로 반환한다")
	void findReturnsEnumListsWhenNoTopics() {
		OnboardingMetadataResult result = service.find();

		assertThat(result.topics()).isEmpty();
		assertThat(result.breeds()).isNotEmpty();
		assertThat(result.personalities()).isNotEmpty();
		assertThat(result.mbtis()).isNotEmpty();
	}
}
