package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willReturn;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationRevisionCommand;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionCommand;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmissionStatus;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationReviewPendingException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationSubmissionConflictException;
import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ApplicationTest
@Import(TopicSeeder.class)
class GraduationSubmitterTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final Long USER_ID = 42L;

	@Autowired
	private GraduationSubmitter graduationSubmitter;

	@MockitoSpyBean
	private GraduationSubmissionRepository graduationSubmissionRepository;


	@Autowired
	private TopicSeeder topicSeeder;

	@Test
	void 첫_영상은_미제출_이력을_보존하고_두번째_이력으로_저장한다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		GraduationSubmission graduation = graduationSubmitter.submit(USER_ID, topic.getId(),
				request(key(topic, "first")));
		topicSeeder.flushAndClear();

		assertThat(graduation.getStatus()).isEqualTo(GraduationSubmissionStatus.SUBMITTED);
		assertThat(graduation.getVideoObjectKey()).isEqualTo(key(topic, "first"));
		assertThat(graduationSubmissionRepository.findAll())
				.extracting(GraduationSubmission::getStatus)
				.containsExactly(GraduationSubmissionStatus.NOT_SUBMITTED, GraduationSubmissionStatus.SUBMITTED);
	}

	@Test
	void 검수_대기_중에는_영상을_바꾸지_않고_추가_제출을_거절한다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "first")));
		topicSeeder.flushAndClear();
		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "second"))))
				.isInstanceOf(GraduationReviewPendingException.class);
		assertThat(graduationSubmissionRepository.findAll()).extracting(GraduationSubmission::getVideoObjectKey)
				.containsExactly(null, key(topic, "first"));
	}

	@Test
	void 재제출하면_이전_판정과_재촬영_지시를_보존하고_새_제출은_판정_대기다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		GraduationSubmission first = graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "first")));
		Instant reviewedAt = Instant.parse("2026-10-20T01:00:00Z");
		GraduationSubmission previous = first;
		first.requestRevision(new GraduationRevisionCommand("다시 찍어 주세요.",
				List.of("현관을 보여 주세요.", "강아지 전신을 찍어 주세요."), "https://images.test/images/graduation-retake/3/example.jpg",
				reviewedAt));
		topicSeeder.flushAndClear();

		GraduationSubmission second = graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "second")));
		topicSeeder.flushAndClear();

		GraduationSubmission saved = graduationSubmissionRepository.findById(second.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(GraduationSubmissionStatus.SUBMITTED);
		assertThat(saved.getReview().note()).isNull();
		assertThat(saved.getReviewedAt()).isNull();
		assertThat(graduationSubmissionRepository.findAll())
				.extracting(GraduationSubmission::getVideoObjectKey)
				.containsExactlyInAnyOrder(null, key(topic, "first"), key(topic, "second"));
		previous = graduationSubmissionRepository.findById(previous.getId()).orElseThrow();
		assertThat(previous.getStatus()).isEqualTo(GraduationSubmissionStatus.REVISION_REQUESTED);
		assertThat(previous.getReview().note()).isEqualTo("다시 찍어 주세요.");
		assertThat(previous.getReviewedAt()).isEqualTo(reviewedAt);
		assertThat(previous.getReview().retakeInstructions()).containsExactly("현관을 보여 주세요.", "강아지 전신을 찍어 주세요.");
		assertThat(previous.getRetakeImageUrl()).isEqualTo(
				"https://images.test/images/graduation-retake/3/example.jpg");
		assertThat(saved.getStatus()).isEqualTo(GraduationSubmissionStatus.SUBMITTED);
		assertThat(saved.getReview().retakeInstructions()).isEmpty();
		assertThat(saved.getRetakeImageUrl()).isNull();
	}

	@Test
	void 새_제출을_승인해도_이전_보완_요청은_유지한다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		GraduationSubmission first = graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "first")));
		GraduationSubmission previous = first;
		first.requestRevision(new GraduationRevisionCommand("현관을 보여 주세요.", List.of(), null, Instant.now()));
		topicSeeder.flushAndClear();
		GraduationSubmission second = graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "second")));

		second.approve("잘했어요.", Instant.now());
		topicSeeder.flushAndClear();

		assertThat(graduationSubmissionRepository.findById(previous.getId()).orElseThrow()
				.getStatus()).isEqualTo(GraduationSubmissionStatus.REVISION_REQUESTED);
		GraduationSubmission approved = graduationSubmissionRepository
				.findById(second.getId()).orElseThrow();
		assertThat(approved.getStatus()).isEqualTo(GraduationSubmissionStatus.APPROVED);
		assertThat(approved.getReview().note()).isEqualTo("잘했어요.");
		assertThat(approved.getReviewedAt()).isNotNull();
	}

	@Test
	void 승인된_수료에_다시_제출하면_예외를_던진다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		GraduationSubmission first = graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "first")));
		first.approve(null, Instant.now());
		topicSeeder.flushAndClear();

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "second"))))
				.isInstanceOf(GraduationAlreadyApprovedException.class);
	}

	@Test
	void 다른_사용자의_영상_키로_제출하면_예외를_던지고_아무것도_만들지_않는다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(),
				request("videos/graduation/43/" + topic.getId() + "/x.mp4")))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
		assertThat(graduationSubmissionRepository.findFirstByUserIdAndTopicIdOrderByIdDesc(USER_ID,
				topic.getId())).get().extracting(GraduationSubmission::getStatus)
				.isEqualTo(GraduationSubmissionStatus.NOT_SUBMITTED);
	}

	@Test
	void 다른_토픽으로_올린_영상_키로_제출하면_예외를_던지고_아무것도_만들지_않는다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		String otherTopicKey = "videos/graduation/42/" + (topic.getId() + 1) + "/x.mp4";

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(otherTopicKey)))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
		assertThat(graduationSubmissionRepository.findFirstByUserIdAndTopicIdOrderByIdDesc(USER_ID,
				topic.getId())).get().extracting(GraduationSubmission::getStatus)
				.isEqualTo(GraduationSubmissionStatus.NOT_SUBMITTED);
	}

	@Test
	void 커리큘럼을_다_마치지_않은_토픽에_제출하면_예외를_던진다() {
		Topic topic = topicSeeder.incompleteTopic("분리불안");

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "first"))))
				.isInstanceOf(TopicNotCompletedException.class);
	}

	@Test
	void 같은_토픽의_첫_제출이_동시에_들어와_유니크_제약에_걸리면_충돌_예외로_바꾼다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		jdbcTemplate.execute(
				"create unique index if not exists uk_graduation_submissions_active on graduation_submissions(user_id, topic_id) where status in ('SUBMITTED', 'APPROVED')");
		graduationSubmissionRepository.saveAndFlush(GraduationSubmission.create(
				new GraduationSubmissionCommand(USER_ID, topic.getId(), key(topic, "first"), Instant.now())));
		willReturn(Optional.empty()).given(graduationSubmissionRepository)
				.findFirstByUserIdAndTopicIdOrderByIdDesc(USER_ID, topic.getId());

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(key(topic, "second"))))
				.isInstanceOf(GraduationSubmissionConflictException.class);
	}

	private String key(Topic topic, String name) {
		return "videos/graduation/" + USER_ID + "/" + topic.getId() + "/" + name + ".mp4";
	}

	private GraduationSubmissionRequest request(String objectKey) {
		return new GraduationSubmissionRequest(objectKey);
	}
}
