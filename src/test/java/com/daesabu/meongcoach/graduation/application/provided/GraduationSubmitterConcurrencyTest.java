package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
	private TopicGraduationRepository topicGraduationRepository;

	@Autowired
	private GraduationSubmissionRepository graduationSubmissionRepository;

	@Autowired
	private TopicSeeder topicSeeder;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private TransactionTemplate transaction;
	private TransactionTemplate newTransaction;
	private Long topicId;
	private Long categoryId;
	private Long graduationId;

	@BeforeEach
	void setUp() {
		transaction = new TransactionTemplate(transactionManager);
		newTransaction = new TransactionTemplate(transactionManager);
		newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transaction.executeWithoutResult(status -> {
			Topic topic = topicSeeder.completedTopic(USER_ID, "동시 제출 검증");
			topicId = topic.getId();
			categoryId = topic.getTrainingCategory().getId();
			TopicGraduation graduation = graduationSubmitter.submit(USER_ID, topicId, request("first"));
			graduationId = graduation.getId();
		});
	}

	@AfterEach
	void cleanUp() {
		transaction.executeWithoutResult(status -> {
			jdbcTemplate.update("delete from graduation_submissions where graduation_id = ?", graduationId);
			jdbcTemplate.update("delete from topic_graduations where id = ?", graduationId);
			jdbcTemplate.update("delete from lesson_progress where user_id = ?", USER_ID);
			jdbcTemplate.update("delete from topic_progress where user_id = ?", USER_ID);
			jdbcTemplate.update("delete from lessons where curriculum_id in (select id from curriculums where topic_id = ?)", topicId);
			jdbcTemplate.update("delete from curriculums where topic_id = ?", topicId);
			jdbcTemplate.update("delete from topics where id = ?", topicId);
			jdbcTemplate.update("delete from training_categories where id = ?", categoryId);
		});
	}

	@Test
	void 같은_수료를_먼저_재제출하면_이전_버전의_재제출은_실패하고_기록이_남지_않는다() {
		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
			topicGraduationRepository.findByUserIdAndTopicId(USER_ID, topicId).orElseThrow();
			newTransaction.executeWithoutResult(winner -> graduationSubmitter.submit(USER_ID, topicId, request("winner")));

			graduationSubmitter.submit(USER_ID, topicId, request("stale"));
		})).isInstanceOf(OptimisticLockingFailureException.class);

		TopicGraduation saved = topicGraduationRepository.findById(graduationId).orElseThrow();
		assertThat(saved.getAttemptNo()).isEqualTo(2);
		assertThat(saved.getVideoObjectKey()).isEqualTo(key("winner"));
		assertThat(graduationSubmissionRepository.findAll())
				.filteredOn(submission -> submission.getGraduationId().equals(graduationId))
				.extracting(GraduationSubmission::getAttemptNo, GraduationSubmission::getVideoObjectKey)
				.containsExactlyInAnyOrder(tuple(1, key("first")), tuple(2, key("winner")));
	}

	@Test
	void 먼저_승인된_수료는_이전_버전의_재제출로_덮어쓰지_않는다() {
		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
			topicGraduationRepository.findByUserIdAndTopicId(USER_ID, topicId).orElseThrow();
			newTransaction.executeWithoutResult(winner -> jdbcTemplate.update(
					"update topic_graduations set status = 'APPROVED', reviewed_at = now(), approved_at = now(), version = version + 1 where id = ?",
					graduationId));

			graduationSubmitter.submit(USER_ID, topicId, request("stale"));
		})).isInstanceOf(OptimisticLockingFailureException.class);

		TopicGraduation saved = topicGraduationRepository.findById(graduationId).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(GraduationStatus.APPROVED);
		assertThat(saved.getAttemptNo()).isEqualTo(1);
		assertThat(graduationSubmissionRepository.findAll())
				.filteredOn(submission -> submission.getGraduationId().equals(graduationId))
				.extracting(GraduationSubmission::getVideoObjectKey)
				.containsExactly(key("first"));
	}

	private GraduationSubmissionRequest request(String name) {
		String objectKey = key(name);
		return new GraduationSubmissionRequest(objectKey);
	}

	private String key(String name) {
		return "videos/graduation/" + USER_ID + "/" + topicId + "/" + name + ".mp4";
	}
}
