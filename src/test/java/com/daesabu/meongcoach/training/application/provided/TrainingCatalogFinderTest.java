package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.EntitlementFixture;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCategory;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogEntry;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogLesson;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogTopic;
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
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class TrainingCatalogFinderTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private TrainingCatalogFinder trainingCatalogFinder;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 카테고리_토픽_커리큘럼_레슨을_정렬_순서대로_반환한다() {
		TrainingCategory later = saveCategory("나중 카테고리", 2, null);
		TrainingCategory first = saveCategory("먼저 카테고리", 1, null);
		saveTopic(first, "둘째 토픽", 2);
		Topic firstTopic = saveTopic(first, "첫째 토픽", 1);
		saveCurriculum(firstTopic, "둘째 커리큘럼", 2, false);
		Curriculum firstCurriculum = saveCurriculum(firstTopic, "첫째 커리큘럼", 1, false);
		saveLesson(firstCurriculum, "둘째 레슨", 2, 10);
		saveLesson(firstCurriculum, "첫째 레슨", 1, 5);
		saveTopic(later, "다른 토픽", 1);
		flushAndClear();

		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(USER_ID);

		assertThat(catalog.categories()).extracting(CatalogCategory::title)
				.containsExactly("먼저 카테고리", "나중 카테고리");
		CatalogCategory category = catalog.categories().getFirst();
		assertThat(category.topics()).extracting(CatalogTopic::title)
				.containsExactly("첫째 토픽", "둘째 토픽");
		CatalogTopic topic = category.topics().getFirst();
		assertThat(topic.curriculums()).extracting(CatalogCurriculum::title)
				.containsExactly("첫째 커리큘럼", "둘째 커리큘럼");
		assertThat(topic.curriculums().getFirst().lessons()).extracting(CatalogLesson::title)
				.containsExactly("첫째 레슨", "둘째 레슨");
	}

	@Test
	void 카테고리_토픽_커리큘럼_레슨의_식별자와_속성을_담는다() {
		TrainingCategory category = saveCategory("퍼피 교육", 3, EntitlementType.PUPPY);
		Topic topic = saveTopic(category, "앉아", 4);
		Curriculum curriculum = saveCurriculum(topic, "앉아 기초", 5, true);
		Lesson lesson = saveLesson(curriculum, "간식으로 유도하기", 6, 7);
		flushAndClear();

		CatalogCategory found = trainingCatalogFinder.findCatalog(USER_ID).categories().getFirst();

		assertThat(found.id()).isEqualTo(category.getId());
		assertThat(found.sortOrder()).isEqualTo(3);
		assertThat(found.requiredEntitlementType()).isEqualTo(EntitlementType.PUPPY);
		CatalogTopic foundTopic = found.topics().getFirst();
		assertThat(foundTopic.id()).isEqualTo(topic.getId());
		assertThat(foundTopic.sortOrder()).isEqualTo(4);
		CatalogCurriculum foundCurriculum = foundTopic.curriculums().getFirst();
		assertThat(foundCurriculum.id()).isEqualTo(curriculum.getId());
		assertThat(foundCurriculum.description()).isEqualTo("앉아 기초 설명");
		assertThat(foundCurriculum.sortOrder()).isEqualTo(5);
		assertThat(foundCurriculum.premium()).isTrue();
		CatalogLesson foundLesson = foundCurriculum.lessons().getFirst();
		assertThat(foundLesson.id()).isEqualTo(lesson.getId());
		assertThat(foundLesson.sortOrder()).isEqualTo(6);
		assertThat(foundLesson.estimatedMinutes()).isEqualTo(7);
	}

	@Test
	void 레슨_수와_예상_시간을_반환한다() {
		TrainingCategory category = saveCategory("기본 교육", 1, null);
		Topic topic = saveTopic(category, "앉아", 1);
		Curriculum curriculum = saveCurriculum(topic, "앉아 기초", 1, false);
		saveLesson(curriculum, "첫째 레슨", 1, 5);
		saveLesson(curriculum, "둘째 레슨", 2, 8);
		saveLesson(curriculum, "셋째 레슨", 3, 12);
		flushAndClear();

		CatalogCurriculum found = firstCurriculum(trainingCatalogFinder.findCatalog(USER_ID));

		assertThat(found.lessons()).hasSize(3);
		assertThat(found.lessons()).extracting(CatalogLesson::estimatedMinutes)
				.containsExactly(5, 8, 12);
	}

	@Test
	void 이용권이_있으면_유료_커리큘럼이_잠기지_않는다() {
		saveLockableCatalog();
		grant(EntitlementType.PUPPY);
		flushAndClear();

		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(USER_ID);

		CatalogCurriculum premium = curriculumByTitle(catalog, "퍼피 유료");
		assertThat(premium.premium()).isTrue();
		assertThat(premium.locked()).isFalse();
	}

	@Test
	void 이용권이_없으면_유료_커리큘럼을_목록에_남기고_잠근다() {
		saveLockableCatalog();
		flushAndClear();

		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(USER_ID);

		CatalogCurriculum premium = curriculumByTitle(catalog, "퍼피 유료");
		assertThat(premium.premium()).isTrue();
		assertThat(premium.locked()).isTrue();
	}

	@Test
	void 다른_종류의_이용권만_있으면_잠긴다() {
		saveLockableCatalog();
		grant(EntitlementType.JUNIOR);
		flushAndClear();

		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(USER_ID);

		assertThat(curriculumByTitle(catalog, "퍼피 유료").locked()).isTrue();
		assertThat(curriculumByTitle(catalog, "주니어 유료").locked()).isFalse();
	}

	@Test
	void 무료_카테고리와_무료_커리큘럼은_이용권이_없어도_잠기지_않는다() {
		saveLockableCatalog();
		flushAndClear();

		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(USER_ID);

		assertThat(curriculumByTitle(catalog, "기본 무료").locked()).isFalse();
		assertThat(curriculumByTitle(catalog, "퍼피 무료").locked()).isFalse();
	}

	@Test
	void 열린_커리큘럼만_카탈로그_순서로_반환한다() {
		saveLockableCatalog();
		flushAndClear();

		List<CatalogCurriculum> open = trainingCatalogFinder.findCatalog(USER_ID).openCurriculums();

		assertThat(open).extracting(CatalogCurriculum::title)
				.containsExactly("기본 무료", "퍼피 무료");
	}

	@Test
	void 식별자로_커리큘럼과_소속_토픽_카테고리를_찾는다() {
		TrainingCategory category = saveCategory("기본 교육", 1, null);
		Topic topic = saveTopic(category, "앉아", 1);
		Curriculum curriculum = saveCurriculum(topic, "앉아 기초", 1, false);
		flushAndClear();

		Optional<CatalogEntry> entry = trainingCatalogFinder.findCatalog(USER_ID)
				.findCurriculum(curriculum.getId());

		assertThat(entry).isPresent();
		assertThat(entry.get().category().id()).isEqualTo(category.getId());
		assertThat(entry.get().topic().id()).isEqualTo(topic.getId());
		assertThat(entry.get().curriculum().id()).isEqualTo(curriculum.getId());
	}

	@Test
	void 잠긴_커리큘럼도_식별자로_찾는다() {
		saveLockableCatalog();
		flushAndClear();
		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(USER_ID);
		Long lockedId = curriculumByTitle(catalog, "퍼피 유료").id();

		Optional<CatalogEntry> entry = catalog.findCurriculum(lockedId);

		assertThat(entry).isPresent();
		assertThat(entry.get().curriculum().locked()).isTrue();
	}

	@Test
	void 없는_식별자로_찾으면_빈_값을_반환한다() {
		saveLockableCatalog();
		flushAndClear();

		Optional<CatalogEntry> entry = trainingCatalogFinder.findCatalog(USER_ID).findCurriculum(-1L);

		assertThat(entry).isEmpty();
	}

	@Test
	void 모든_레슨의_식별자를_카탈로그_순서로_반환한다() {
		TrainingCategory category = saveCategory("기본 교육", 1, null);
		Topic topic = saveTopic(category, "앉아", 1);
		Curriculum first = saveCurriculum(topic, "첫째 커리큘럼", 1, false);
		Curriculum second = saveCurriculum(topic, "둘째 커리큘럼", 2, false);
		Lesson secondLesson = saveLesson(first, "둘째 레슨", 2, 5);
		Lesson firstLesson = saveLesson(first, "첫째 레슨", 1, 5);
		Lesson thirdLesson = saveLesson(second, "셋째 레슨", 1, 5);
		flushAndClear();

		List<Long> lessonIds = trainingCatalogFinder.findCatalog(USER_ID).allLessonIds();

		assertThat(lessonIds).containsExactly(firstLesson.getId(), secondLesson.getId(), thirdLesson.getId());
	}

	@Test
	void 등록된_카테고리가_없으면_빈_카탈로그를_반환한다() {
		TrainingCatalogResult catalog = trainingCatalogFinder.findCatalog(USER_ID);

		assertThat(catalog.categories()).isEmpty();
		assertThat(catalog.openCurriculums()).isEmpty();
		assertThat(catalog.allLessonIds()).isEmpty();
	}

	private void saveLockableCatalog() {
		TrainingCategory basic = saveCategory("기본 교육", 1, null);
		TrainingCategory puppy = saveCategory("퍼피 교육", 2, EntitlementType.PUPPY);
		TrainingCategory junior = saveCategory("주니어 교육", 3, EntitlementType.JUNIOR);
		saveCurriculum(saveTopic(basic, "기본 토픽", 1), "기본 무료", 1, false);
		Topic puppyTopic = saveTopic(puppy, "퍼피 토픽", 1);
		saveCurriculum(puppyTopic, "퍼피 무료", 1, false);
		saveCurriculum(puppyTopic, "퍼피 유료", 2, true);
		saveCurriculum(saveTopic(junior, "주니어 토픽", 1), "주니어 유료", 1, true);
	}

	private CatalogCurriculum firstCurriculum(TrainingCatalogResult catalog) {
		return catalog.categories().getFirst().topics().getFirst().curriculums().getFirst();
	}

	private CatalogCurriculum curriculumByTitle(TrainingCatalogResult catalog, String title) {
		return catalog.categories().stream()
				.flatMap(category -> category.topics().stream())
				.flatMap(topic -> topic.curriculums().stream())
				.filter(curriculum -> curriculum.title().equals(title))
				.findFirst()
				.orElseThrow();
	}

	private void grant(EntitlementType type) {
		entitlementRepository.save(EntitlementFixture.create(USER_ID, type, null));
	}

	private TrainingCategory saveCategory(String title, int sortOrder, EntitlementType type) {
		return trainingCategoryRepository.save(TrainingCategoryFixture.create(title, sortOrder, null, null, type));
	}

	private Topic saveTopic(TrainingCategory category, String title, int sortOrder) {
		return topicRepository.save(TopicFixture.create(category, title, sortOrder, null, null, null));
	}

	private Curriculum saveCurriculum(Topic topic, String title, int sortOrder, boolean isPremium) {
		return curriculumRepository.save(
				CurriculumFixture.create(topic, title, sortOrder, null, title + " 설명", isPremium));
	}

	private Lesson saveLesson(Curriculum curriculum, String title, int sortOrder, int estimatedMinutes) {
		return lessonRepository.save(LessonFixture.create(curriculum, title, sortOrder, estimatedMinutes));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
