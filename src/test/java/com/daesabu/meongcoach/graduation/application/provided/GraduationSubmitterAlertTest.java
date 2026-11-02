package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.alert.application.required.AlertSender;
import com.daesabu.meongcoach.alert.domain.Alert;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
@Import(TopicSeeder.class)
class GraduationSubmitterAlertTest {

	private static final Long USER_ID = 99164L;
	private static final long ASYNC_TIMEOUT_MILLIS = 2000;
	private static final long ASYNC_SETTLE_MILLIS = 300;

	@Autowired
	private GraduationSubmitter graduationSubmitter;

	@Autowired
	private AlertSender alertSender;

	@Autowired
	private TopicSeeder topicSeeder;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private TransactionTemplate transaction;
	private Long topicId;
	private Long categoryId;

	@BeforeEach
	void setUp() {
		transaction = new TransactionTemplate(transactionManager);
		transaction.executeWithoutResult(status -> {
			Topic topic = topicSeeder.completedTopic(USER_ID, "제출 알림 검증");
			topicId = topic.getId();
			categoryId = topic.getTrainingCategory().getId();
		});
	}

	@AfterEach
	void cleanUp() {
		transaction.executeWithoutResult(status -> {
			jdbcTemplate.update("delete from graduation_submissions where user_id = ?", USER_ID);
			jdbcTemplate.update("delete from lesson_progress where user_id = ?", USER_ID);
			jdbcTemplate.update(
					"delete from lessons where curriculum_id in (select id from curriculums where topic_id = ?)",
					topicId);
			jdbcTemplate.update("delete from curriculums where topic_id = ?", topicId);
			jdbcTemplate.update("delete from topics where id = ?", topicId);
			jdbcTemplate.update("delete from training_categories where id = ?", categoryId);
		});
	}

	@Test
	void 제출이_커밋되면_제출_정보를_알린다() {
		GraduationSubmission submission = graduationSubmitter.submit(USER_ID, topicId, request());

		ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
		verify(alertSender, timeout(ASYNC_TIMEOUT_MILLIS)).send(captor.capture());
		Alert alert = captor.getValue();
		assertThat(alert.title()).isEqualTo("실습 인증 제출");
		assertThat(alert.fields()).containsExactly(
				AlertField.of("submissionId", submission.getId()),
				AlertField.of("userId", USER_ID),
				AlertField.of("topicId", topicId));
	}

	@Test
	void 제출_트랜잭션이_롤백되면_알리지_않는다() {
		transaction.executeWithoutResult(status -> {
			graduationSubmitter.submit(USER_ID, topicId, request());
			status.setRollbackOnly();
		});

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}

	@Test
	void 제출이_거절되면_알리지_않는다() {
		GraduationSubmissionRequest invalidKey = new GraduationSubmissionRequest("videos/graduation/invalid.mp4");

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topicId, invalidKey))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}

	private GraduationSubmissionRequest request() {
		return new GraduationSubmissionRequest("videos/graduation/" + USER_ID + "/" + topicId + "/first.mp4");
	}
}
