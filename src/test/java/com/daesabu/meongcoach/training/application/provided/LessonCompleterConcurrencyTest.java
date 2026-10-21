package com.daesabu.meongcoach.training.application.provided;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.LessonProgress;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class LessonCompleterConcurrencyTest {
	private static final Long USER_ID = 987654L;

	@Autowired
	private LessonCompleter lessonCompleter;

	@Autowired
	private LessonProgressRepository repository;

	@Autowired
	private TransactionTemplate transactions;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private TrainingCategoryRepository categoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository contentRepository;

	private final List<Long> categoryIds = new ArrayList<>();
	private final ExecutorService executor = Executors.newFixedThreadPool(2);
	private final CompletableFuture<Void> release = new CompletableFuture<>();

	@AfterEach
	void tearDown() {
		release.complete(null);
		executor.close();
		jdbc.update("delete from lesson_progress where user_id = ?", USER_ID);
		for (Long categoryId : categoryIds) {
			jdbc.update("""
					delete from lessons where curriculum_id in
					(select c.id from curriculums c join topics t on t.id = c.topic_id
						where t.training_category_id = ?)
					""", categoryId);
			jdbc.update("""
					delete from curriculums where topic_id in
					(select id from topics where training_category_id = ?)
					""", categoryId);
			jdbc.update("delete from topics where training_category_id = ?", categoryId);
			jdbc.update("delete from training_categories where id = ?", categoryId);
		}
	}

	@Test
	void 동시_최초_완료는_하나의_레슨_진행에_두_번_기록된다() throws Exception {
		Lesson lesson = saveLesson();
		CompletableFuture<Void> acquired = new CompletableFuture<>();
		Future<?> first = executor.submit(() -> transactions.executeWithoutResult(status -> {
			repository.lockProgress(USER_ID, lesson.getId());
			acquired.complete(null);
			release.join();
			lessonCompleter.completeLesson(USER_ID, lesson.getId());
		}));
		acquired.get(5, SECONDS);

		Future<Integer> second = executor.submit(() -> lessonCompleter.completeLesson(USER_ID, lesson.getId()));

		assertThatThrownBy(() -> second.get(500, MILLISECONDS)).isInstanceOf(TimeoutException.class);
		release.complete(null);
		first.get(5, SECONDS);
		assertThat(second.get(5, SECONDS)).isEqualTo(2);
		assertThat(repository.findByUserIdAndLessonId(USER_ID, lesson.getId()))
				.get().extracting(LessonProgress::getCompletedCount).isEqualTo(2);
	}

	@Test
	void 같은_커리큘럼의_다른_레슨은_잠금을_기다리지_않는다() throws Exception {
		Lesson locked = saveLesson();
		Lesson lesson = transactions.execute(status -> {
			Lesson other = LessonFixture.create(locked.getCurriculum(), "다른 레슨", 2, 5);
			return contentRepository.save(other);
		});
		CompletableFuture<Void> acquired = new CompletableFuture<>();
		executor.submit(() -> transactions.executeWithoutResult(status -> {
			repository.lockProgress(USER_ID, locked.getId());
			acquired.complete(null);
			release.join();
		}));
		acquired.get(5, SECONDS);

		Future<Integer> completion = executor.submit(() -> lessonCompleter.completeLesson(USER_ID, lesson.getId()));

		assertThat(completion.get(5, SECONDS)).isOne();
	}

	private Lesson saveLesson() {
		return transactions.execute(status -> {
			TrainingCategory category = categoryRepository.save(
					TrainingCategoryFixture.create("동시 완료", 1, null, null));
			categoryIds.add(category.getId());
			Topic topic = topicRepository.save(TopicFixture.create(category, "앉아", 1, null, null, null));
			Curriculum curriculum = curriculumRepository.save(
					CurriculumFixture.create(topic, "앉아 단계", 1, null, null));
			Lesson lesson = LessonFixture.create(curriculum, "앉아 레슨", 1, 5);
			return contentRepository.save(lesson);
		});
	}
}
