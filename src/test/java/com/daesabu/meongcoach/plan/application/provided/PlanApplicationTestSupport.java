package com.daesabu.meongcoach.plan.application.provided;

import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.Dogs;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class PlanApplicationTestSupport {

	protected static final Long PRIOR_TOPIC_ID = 9999L;
	protected static final String ORIGINAL_EXPECTATION = "기존 기대사항";
	protected static final String NEW_EXPECTATION = "산책할 때 줄을 당기지 않았으면 좋겠어요";

	@Autowired
	protected TrainingPlanRepository trainingPlanRepository;

	@Autowired
	protected DogRepository dogRepository;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected UserProfileRepository userProfileRepository;

	@Autowired
	protected TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	protected TopicRepository topicRepository;

	@Autowired
	protected CurriculumRepository curriculumRepository;

	@Autowired
	protected LessonRepository lessonRepository;

	@Autowired
	protected EntityManager entityManager;

	protected Long userId;
	protected Long dogId;
	protected Long sitTopicId;
	protected Long comeTopicId;
	protected Long lockedTopicId;
	protected Long sitBasicId;
	protected Long sitAdvancedId;
	protected Long comeBasicId;
	protected List<Long> sitBasicLessonIds;
	protected List<Long> sitAdvancedLessonIds;

	@BeforeEach
	void seedPlanScenario() {
		seedUser();
		seedDog();
		seedCatalog();
		entityManager.flush();
		entityManager.clear();
	}

	protected PlanCreateRequest manualRequest(boolean replace, Long... topicIds) {
		return new PlanCreateRequest(dogId, "MANUAL", List.of(topicIds), null, 10, 3, replace);
	}

	protected PlanCreateRequest aiRequest(String expectation, boolean replace) {
		return new PlanCreateRequest(dogId, "AI", null, expectation, 10, 3, replace);
	}

	protected List<TrainingPlan> plansOf(PlanStatus status) {
		return trainingPlanRepository.findByUserIdAndStatus(userId, status);
	}

	private void seedUser() {
		User user = userRepository.save(User.registerUser());
		userProfileRepository.save(UserProfile.create(user,
				new UserProfileCreateCommand("멍멍이집사", null, null, null, null, Set.of(PRIOR_TOPIC_ID), Set.of())));
		userId = user.getId();
	}

	private void seedDog() {
		DogRegisterCommand command = new DogRegisterCommand(userId, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.now().minusMonths(18), false, new BigDecimal("4.5"), null, ORIGINAL_EXPECTATION);
		Dog dog = dogRepository.save(new Dogs(List.of()).register(command));
		dogId = dog.getId();
	}

	private void seedCatalog() {
		TrainingCategory basic = trainingCategoryRepository
				.save(TrainingCategoryFixture.create("기본 교육", 1, null, null, null));
		TrainingCategory puppy = trainingCategoryRepository
				.save(TrainingCategoryFixture.create("퍼피 교육", 2, null, null, EntitlementType.PUPPY));
		Topic sit = topicRepository.save(TopicFixture.create(basic, "앉아", 1, null, null, null));
		Topic come = topicRepository.save(TopicFixture.create(basic, "이리와", 2, null, null, null));
		Topic locked = topicRepository.save(TopicFixture.create(puppy, "퍼피 유료", 1, null, null, null));
		Curriculum sitBasic = saveCurriculum(sit, "앉아 기초", 1, false);
		Curriculum sitAdvanced = saveCurriculum(sit, "앉아 심화", 2, false);
		Curriculum comeBasic = saveCurriculum(come, "이리와 기초", 1, false);
		Curriculum premium = saveCurriculum(locked, "퍼피 유료 커리큘럼", 1, true);
		sitBasicLessonIds = saveLessons(sitBasic);
		sitAdvancedLessonIds = saveLessons(sitAdvanced);
		saveLessons(comeBasic);
		saveLessons(premium);
		sitTopicId = sit.getId();
		comeTopicId = come.getId();
		lockedTopicId = locked.getId();
		sitBasicId = sitBasic.getId();
		sitAdvancedId = sitAdvanced.getId();
		comeBasicId = comeBasic.getId();
	}

	private Curriculum saveCurriculum(Topic topic, String title, int sortOrder, boolean premium) {
		return curriculumRepository.save(CurriculumFixture.create(topic, title, sortOrder, null, title + " 설명", premium));
	}

	private List<Long> saveLessons(Curriculum curriculum) {
		Lesson first = lessonRepository.save(LessonFixture.create(curriculum, curriculum.getTitle() + " 레슨1", 1, 5));
		Lesson second = lessonRepository.save(LessonFixture.create(curriculum, curriculum.getTitle() + " 레슨2", 2, 5));
		return List.of(first.getId(), second.getId());
	}
}
