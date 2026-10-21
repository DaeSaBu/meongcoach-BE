package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionStatus;
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
			jdbcTemplate.update("delete from graduation_submissions where id = ?", submissionId);
			jdbcTemplate.update("delete from lesson_progress where user_id = ?", USER_ID);
			jdbcTemplate.update("delete from topic_progress where user_id = ?", USER_ID);
			jdbcTemplate.update("delete from lessons where curriculum_id in (select id from curriculums where topic_id = ?)", topicId);
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
    fresh.approve("승인", java.time.Instant.now());
   });
   stale.requestRevision(new com.daesabu.meongcoach.graduation.domain.GraduationRevisionCommand("보완", java.util.List.of(), null, java.time.Instant.now()));
  })).isInstanceOf(OptimisticLockingFailureException.class);
  GraduationSubmission saved = graduationSubmissionRepository.findById(submissionId).orElseThrow();
  assertThat(saved.getStatus()).isEqualTo(GraduationSubmissionStatus.APPROVED);
  assertThat(saved.getReview().note()).isEqualTo("승인");
 }

	private GraduationSubmissionRequest request(String name) {
		String objectKey = key(name);
		return new GraduationSubmissionRequest(objectKey);
	}

	private String key(String name) {
		return "videos/graduation/" + USER_ID + "/" + topicId + "/" + name + ".mp4";
	}

}
