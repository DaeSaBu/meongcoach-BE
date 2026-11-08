package com.daesabu.meongcoach.home.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.plan.domain.Plan;
import com.daesabu.meongcoach.plan.domain.PlanCreateCommand;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.Topic;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class QuickTopicFinderTest {

	private static final Long USER_ID = 42L;
	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));
	private static final List<Long> STARTER_UNDER_TWO = List.of(1601L, 1204L, 1303L, 1201L, 1602L);
	private static final List<Long> STARTER_OVER_TWO = List.of(1101L, 1104L, 1102L, 1406L, 1411L);

	@Autowired
	private QuickTopicFinder finder;

	@Autowired
	private EntityManager entityManager;

	private final Map<Long, List<Lesson>> lessonsByTopic = new HashMap<>();

	@BeforeEach
	void setUp() {
		saveCategory(16L, "훈련 기본기", 1);
		saveCategory(15L, "퍼피 교육", 2);
		saveCategory(12L, "생활 교육", 3);
		saveCategory(13L, "사회화", 4);
		saveCategory(11L, "기본 훈련", 5);
		saveCategory(14L, "개인기", 6);
		saveTopic(1601L, 16L, 1);
		saveTopic(1602L, 16L, 2);
		saveTopic(1501L, 15L, 1);
		saveTopic(1201L, 12L, 1);
		saveTopic(1204L, 12L, 2);
		saveTopic(1303L, 13L, 1);
		saveTopic(1101L, 11L, 1);
		saveTopic(1102L, 11L, 2);
		saveTopic(1104L, 11L, 3);
		saveTopic(1105L, 11L, 4);
		saveTopic(1406L, 14L, 1);
		saveTopic(1411L, 14L, 2);
		flushAndClear();
	}

	@Test
	void 학습_기록이_없으면_두_살_전_강아지에게_고정_토픽_5개를_반환한다() {
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(23));

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(topicIds(result)).containsExactlyElementsOf(STARTER_UNDER_TWO);
		assertThat(result).extracting(QuickTopicResult::role).containsOnly(QuickTopicRole.STARTER);
	}

	@Test
	void 학습_기록이_없으면_두_살_이후_강아지에게_다른_고정_토픽_5개를_반환한다() {
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(24));

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(topicIds(result)).containsExactlyElementsOf(STARTER_OVER_TWO);
		assertThat(result).extracting(QuickTopicResult::role).containsOnly(QuickTopicRole.STARTER);
	}

	@Test
	void 생일이_없는_강아지는_두_살_전으로_본다() {
		Long dogId = saveDog(USER_ID, null);

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(topicIds(result)).containsExactlyElementsOf(STARTER_UNDER_TWO);
	}

	@Test
	void 고정_토픽_가운데_없는_토픽은_빼고_반환한다() {
		entityManager.createNativeQuery("DELETE FROM lessons WHERE curriculum_id IN "
				+ "(SELECT id FROM curriculums WHERE topic_id = 1602)").executeUpdate();
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(topicIds(result)).containsExactly(1601L, 1204L, 1303L, 1201L);
	}

	@Test
	void 사진이_없는_고정_토픽은_빼고_반환한다() {
		clearThumbnail(1204L);
		clearBlankThumbnail(1602L);
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(topicIds(result)).containsExactly(1601L, 1303L, 1201L);
	}

	@Test
	void 사진이_없는_토픽은_이어서와_새_토픽에서_뺀다() {
		complete(USER_ID, 1101L, 0, LocalDateTime.of(2026, 11, 1, 9, 0));
		complete(USER_ID, 1102L, 0, LocalDateTime.of(2026, 11, 2, 9, 0));
		clearThumbnail(1102L);
		clearThumbnail(1201L);
		clearBlankThumbnail(1303L);
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(result.getFirst().topic().id()).isEqualTo(1101L);
		assertThat(result.getFirst().role()).isEqualTo(QuickTopicRole.CONTINUE);
		assertThat(topicIds(result)).doesNotContain(1102L, 1201L, 1303L);
		assertThat(result).allSatisfy(item -> assertThat(item.topic().thumbnailUrl()).isNotBlank());
	}

	@Test
	void 다른_회원의_학습_기록은_가입_직후_판단에_쓰지_않는다() {
		complete(43L, 1101L, 0, LocalDateTime.of(2026, 11, 1, 9, 0));
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(result).extracting(QuickTopicResult::role).containsOnly(QuickTopicRole.STARTER);
	}

	@Test
	void 두_살_이후_강아지에게는_훈련_기본기와_퍼피_교육_토픽을_보여_주지_않는다() {
		completeAll(1105L);
		Long dogId = saveDog(USER_ID, TODAY.minusYears(3));

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(result).hasSize(5);
		assertThat(result).extracting(QuickTopicResult::role).containsOnly(QuickTopicRole.NEW);
		assertThat(topicIds(result)).doesNotContain(1601L, 1602L, 1501L, 1105L);
	}

	@Test
	void 두_살_전_강아지에게는_모든_카테고리의_새_토픽을_보여_준다() {
		completeAll(1101L);
		completeAll(1102L);
		completeAll(1104L);
		completeAll(1105L);
		completeAll(1406L);
		completeAll(1411L);
		completeAll(1303L);
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(topicIds(result)).containsExactlyInAnyOrder(1601L, 1602L, 1501L, 1201L, 1204L);
	}

	@Test
	void 플랜_밖에서_가장_최근에_학습한_진행_중_토픽을_첫_칸에_이어서로_반환한다() {
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));
		complete(USER_ID, 1101L, 0, LocalDateTime.of(2026, 11, 7, 9, 0));
		complete(USER_ID, 1102L, 0, LocalDateTime.of(2026, 11, 5, 9, 0));
		complete(USER_ID, 1104L, 0, LocalDateTime.of(2026, 11, 3, 9, 0));
		savePlan(dogId, 1101L);

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(result.getFirst().topic().id()).isEqualTo(1102L);
		assertThat(result.getFirst().role()).isEqualTo(QuickTopicRole.CONTINUE);
		assertThat(result.getFirst().topic().completedLessons()).isEqualTo(1);
		assertThat(result.subList(1, result.size())).extracting(QuickTopicResult::role)
				.containsOnly(QuickTopicRole.NEW);
		assertThat(topicIds(result)).doesNotContain(1101L, 1104L);
		assertThat(result).hasSize(5);
	}

	@Test
	void 플랜_밖에_진행_중인_토픽이_없으면_플랜_안의_진행_중_토픽을_이어서로_반환한다() {
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));
		complete(USER_ID, 1101L, 0, LocalDateTime.of(2026, 11, 7, 9, 0));
		completeAll(1102L);
		savePlan(dogId, 1101L);

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(result.getFirst().topic().id()).isEqualTo(1101L);
		assertThat(result.getFirst().role()).isEqualTo(QuickTopicRole.CONTINUE);
	}

	@Test
	void 진행_중인_토픽이_없으면_모두_새_토픽으로_채우고_시작한_토픽과_플랜_토픽은_뺀다() {
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));
		completeAll(1101L);
		savePlan(dogId, 1102L);

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(result).hasSize(5);
		assertThat(result).extracting(QuickTopicResult::role).containsOnly(QuickTopicRole.NEW);
		assertThat(topicIds(result)).doesNotContain(1101L, 1102L);
		assertThat(result).allSatisfy(item -> assertThat(item.topic().completedLessons()).isZero());
	}

	@Test
	void 완료한_플랜의_토픽은_새_토픽에서_빼지_않는다() {
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));
		completeAll(1101L);
		completeAll(1102L);
		completeAll(1104L);
		completeAll(1105L);
		completeAll(1406L);
		completeAll(1411L);
		completeAll(1303L);
		Plan plan = savePlan(dogId, 1201L);
		plan.complete(Instant.now());
		flushAndClear();

		List<QuickTopicResult> result = finder.find(USER_ID, dogId);

		assertThat(topicIds(result)).contains(1201L);
	}

	@Test
	void 같은_날_같은_회원에게는_같은_순서로_반환한다() {
		Long dogId = saveDog(USER_ID, TODAY.minusMonths(6));
		completeAll(1101L);

		List<Long> first = topicIds(finder.find(USER_ID, dogId));
		List<Long> second = topicIds(finder.find(USER_ID, dogId));

		assertThat(first).isEqualTo(second);
	}

	@Test
	void 다른_회원의_강아지로는_조회할_수_없다() {
		Long dogId = saveDog(43L, TODAY.minusMonths(6));

		assertThatThrownBy(() -> finder.find(USER_ID, dogId)).isInstanceOf(DogNotFoundException.class);
	}

	private List<Long> topicIds(List<QuickTopicResult> result) {
		return result.stream().map(item -> item.topic().id()).toList();
	}

	private void saveCategory(Long id, String title, int sortOrder) {
		entityManager.createNativeQuery("INSERT INTO training_categories "
						+ "(id, title, description, icon_url, sort_order, created_at, updated_at) "
						+ "VALUES (:id, :title, '', '', :sortOrder, now(), now())")
				.setParameter("id", id)
				.setParameter("title", title)
				.setParameter("sortOrder", sortOrder)
				.executeUpdate();
	}

	private void saveTopic(Long id, Long categoryId, int sortOrder) {
		entityManager.createNativeQuery("INSERT INTO topics "
						+ "(id, title, description, detail, icon_url, thumbnail_url, sort_order, training_category_id, created_at, updated_at) "
						+ "VALUES (:id, :title, '', '', '', :thumbnailUrl, :sortOrder, :categoryId, now(), now())")
				.setParameter("id", id)
				.setParameter("title", "토픽 " + id)
				.setParameter("thumbnailUrl", "https://cdn.example.com/topics/thumbnails/" + id + ".jpg")
				.setParameter("sortOrder", sortOrder)
				.setParameter("categoryId", categoryId)
				.executeUpdate();
		Topic topic = entityManager.find(Topic.class, id);
		Curriculum curriculum = CurriculumFixture.create(topic, "커리큘럼 " + id, 1, null, null);
		entityManager.persist(curriculum);
		List<Lesson> lessons = new ArrayList<>();
		for (int order = 1; order <= 2; order++) {
			Lesson lesson = LessonFixture.create(curriculum, "레슨 " + order, order, 5);
			entityManager.persist(lesson);
			lessons.add(lesson);
		}
		lessonsByTopic.put(id, lessons);
	}

	private void clearThumbnail(Long topicId) {
		entityManager.createNativeQuery("UPDATE topics SET thumbnail_url = NULL WHERE id = :id")
				.setParameter("id", topicId)
				.executeUpdate();
	}

	private void clearBlankThumbnail(Long topicId) {
		entityManager.createNativeQuery("UPDATE topics SET thumbnail_url = '  ' WHERE id = :id")
				.setParameter("id", topicId)
				.executeUpdate();
	}

	private void complete(Long userId, Long topicId, int lessonIndex, LocalDateTime completedAt) {
		LessonProgress progress = LessonProgress.start(userId, lessonsByTopic.get(topicId).get(lessonIndex).getId());
		progress.complete();
		entityManager.persist(progress);
		entityManager.flush();
		entityManager.createNativeQuery("UPDATE lesson_progress SET updated_at = :completedAt WHERE id = :id")
				.setParameter("completedAt", completedAt)
				.setParameter("id", progress.getId())
				.executeUpdate();
	}

	private void completeAll(Long topicId) {
		complete(USER_ID, topicId, 0, LocalDateTime.of(2026, 10, 1, 9, 0));
		complete(USER_ID, topicId, 1, LocalDateTime.of(2026, 10, 1, 9, 30));
	}

	private Long saveDog(Long userId, LocalDate birthDate) {
		Dog dog = Dog.register(new DogRegisterCommand(userId, "보리", new Breed("믹스"), DogSex.MALE, birthDate, null,
				null, null));
		entityManager.persist(dog);
		flushAndClear();
		return dog.getId();
	}

	private Plan savePlan(Long dogId, Long topicId) {
		Long curriculumId = lessonsByTopic.get(topicId).getFirst().getCurriculum().getId();
		PlanCreateCommand command = new PlanCreateCommand(USER_ID, dogId, "보리의 2주 훈련 플랜", "목표", 10, 3);
		Plan plan = Plan.create(command, List.of(curriculumId));
		entityManager.persist(plan);
		flushAndClear();
		return entityManager.find(Plan.class, plan.getId());
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
