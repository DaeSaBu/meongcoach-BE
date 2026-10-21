package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationRevisionCommand;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionStatus;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationSubmissionConflictException;
import com.daesabu.meongcoach.progress.application.required.LessonProgressRepository;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.domain.Topic;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
@Import(TopicSeeder.class)
class GraduationSubmitterConcurrencyTest {

	private static final Long USER_ID = 99163L;

	@Autowired
	private GraduationSubmitter graduationSubmitter;


	@Autowired
	private GraduationSubmissionRepository graduationSubmissionRepository;

	@Autowired
	private TopicSeeder topicSeeder;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private LessonCompleter lessonCompleter;

	@Autowired
	private LessonProgressRepository lessonProgressRepository;

	private TransactionTemplate transaction;
	private TransactionTemplate newTransaction;
	private Long topicId;
	private Long categoryId;
	private Long submissionId;

	@BeforeEach
	void setUp() {
		transaction = new TransactionTemplate(transactionManager);
		newTransaction = new TransactionTemplate(transactionManager);
		newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transaction.executeWithoutResult(status -> {
			Topic topic = topicSeeder.completedTopic(USER_ID, "동시 제출 검증");
			topicId = topic.getId();
			categoryId = topic.getTrainingCategory().getId();
			GraduationSubmission graduation = graduationSubmitter.submit(USER_ID, topicId, request("first"));
			submissionId = graduation.getId();
		});
	}

	@AfterEach
	void cleanUp() {
		transaction.executeWithoutResult(status -> {
			jdbcTemplate.update("delete from graduation_submissions where user_id = ?", USER_ID);
			jdbcTemplate.update("delete from lesson_progress where user_id = ?", USER_ID);
			jdbcTemplate.update("delete from topic_progress where user_id = ?", USER_ID);
			jdbcTemplate.update(
					"delete from lessons where curriculum_id in (select id from curriculums where topic_id = ?)",
					topicId);
			jdbcTemplate.update("delete from curriculums where topic_id = ?", topicId);
			jdbcTemplate.update("delete from topics where id = ?", topicId);
			jdbcTemplate.update("delete from training_categories where id = ?", categoryId);
		});
	}

	@Test
	void 동시에_판정하면_이전_버전은_저장되지_않는다() {
		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
			GraduationSubmission stale = graduationSubmissionRepository.findById(submissionId).orElseThrow();
			newTransaction.executeWithoutResult(winner -> {
				GraduationSubmission fresh = graduationSubmissionRepository.findById(submissionId).orElseThrow();
				fresh.approve("승인", Instant.now());
			});
			stale.requestRevision(
					new GraduationRevisionCommand("보완", List.of(),
							null, Instant.now()));
		})).isInstanceOf(OptimisticLockingFailureException.class);
		GraduationSubmission saved = graduationSubmissionRepository.findById(submissionId).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(GraduationSubmissionStatus.APPROVED);
		assertThat(saved.getReview().note()).isEqualTo("승인");
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void 최초_제출과_보완_후_동시_제출은_한_건만_저장한다(boolean revision) throws Exception {
		jdbcTemplate.execute(
				"create unique index if not exists uk_graduation_submissions_active on graduation_submissions(user_id, topic_id) where status in ('SUBMITTED', 'APPROVED')");
		transaction.executeWithoutResult(status -> {
			if (revision) {
				GraduationSubmission first = graduationSubmissionRepository.findById(submissionId).orElseThrow();
				first.requestRevision(new GraduationRevisionCommand("보완",
						List.of(), null, Instant.now()));
				return;
			}
			graduationSubmissionRepository.deleteById(submissionId);
		});
		try (var executor = Executors.newSingleThreadExecutor()) {
			var loser = new AtomicReference<Future<?>>();
			transaction.executeWithoutResult(status -> {
				graduationSubmitter.submit(USER_ID, topicId, request("winner"));
				loser.set(executor.submit(() -> graduationSubmitter.submit(USER_ID, topicId, request("loser"))));
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
				boolean waiting = false;
				while (System.nanoTime() < deadline && !waiting) {
					waiting = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
							"select exists(select 1 from pg_stat_activity where datname = current_database() and wait_event = 'transactionid')",
							Boolean.class));
				}
				assertThat(waiting).isTrue();
				assertThat(loser.get().isDone()).isFalse();
			});
			assertThatThrownBy(() -> loser.get().get(10, TimeUnit.SECONDS))
					.hasCauseInstanceOf(
							GraduationSubmissionConflictException.class);
		}
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from graduation_submissions where user_id = ? and status = 'SUBMITTED'", Integer.class,
				USER_ID)).isEqualTo(1);
		assertThat(
				graduationSubmissionRepository.findFirstByUserIdAndTopicIdOrderByIdDesc(USER_ID, topicId).orElseThrow()
						.getVideoObjectKey()).isEqualTo(key("winner"));
		if (revision) {
			assertThat(
					graduationSubmissionRepository.findById(submissionId).orElseThrow().getReview().note()).isEqualTo(
					"보완");
		}
	}

	@Test
	void 동시_학습_완료는_회원별로_직렬화하고_미제출_이력은_한번만_저장한다() throws Exception {
		jdbcTemplate.update("delete from graduation_submissions where user_id = ?", USER_ID);
		Long lessonId = jdbcTemplate.queryForObject("select l.id from lessons l join curriculums c "
				+ "on c.id = l.curriculum_id where c.topic_id = ?", Long.class, topicId);
		try (var executor = Executors.newSingleThreadExecutor()) {
			var waiting = new AtomicReference<Future<Integer>>();
			transaction.executeWithoutResult(status -> {
				lessonProgressRepository.lockByUserId(USER_ID);
				waiting.set(executor.submit(() -> lessonCompleter.completeLesson(USER_ID, lessonId)));
				assertThatThrownBy(() -> waiting.get().get(250, TimeUnit.MILLISECONDS))
						.isInstanceOf(TimeoutException.class);
				assertThat(lessonCompleter.completeLesson(USER_ID, lessonId)).isEqualTo(2);
			});
			assertThat(waiting.get().get(10, TimeUnit.SECONDS)).isEqualTo(3);
		}
		assertThat(jdbcTemplate.queryForObject("select count(*) from graduation_submissions where user_id = ?",
				Integer.class, USER_ID)).isOne();
		assertThat(graduationSubmissionRepository.findFirstByUserIdAndTopicIdOrderByIdDesc(USER_ID, topicId))
				.get().extracting(GraduationSubmission::getStatus).isEqualTo(GraduationSubmissionStatus.NOT_SUBMITTED);
	}

	private GraduationSubmissionRequest request(String name) {
		String objectKey = key(name);
		return new GraduationSubmissionRequest(objectKey);
	}

	private String key(String name) {
		return "videos/graduation/" + USER_ID + "/" + topicId + "/" + name + ".mp4";
	}
}
