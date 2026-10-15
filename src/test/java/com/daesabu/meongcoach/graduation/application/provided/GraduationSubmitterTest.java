package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.SubmissionNotice;
import com.daesabu.meongcoach.graduation.application.required.SubmissionNotifier;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationSubmissionConflictException;
import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ApplicationTest
@Import(TopicSeeder.class)
class GraduationSubmitterTest {

	private static final Long USER_ID = 42L;
	private static final String FIRST_KEY = "videos/graduation/42/first.mp4";
	private static final String SECOND_KEY = "videos/graduation/42/second.mp4";

	@Autowired
	private GraduationSubmitter graduationSubmitter;

	@MockitoSpyBean
	private TopicGraduationRepository topicGraduationRepository;

	@Autowired
	private GraduationSubmissionRepository graduationSubmissionRepository;

	@Autowired
	private SubmissionNotifier submissionNotifier;

	@Autowired
	private TopicSeeder topicSeeder;

	@Test
	void 첫_제출은_수료_행과_1번째_검수_대기_제출을_만들고_운영자에게_알린다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		GraduationSubmission submission = graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY));

		TopicGraduation graduation = topicGraduationRepository.findByUserIdAndTopicId(USER_ID, topic.getId()).orElseThrow();
		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(submission.getGraduationId()).isEqualTo(graduation.getId());
		assertThat(submission.getAttemptNo()).isEqualTo(1);
		assertThat(submission.getResult()).isEqualTo(SubmissionResult.PENDING);
		assertThat(submission.getVideoObjectKey()).isEqualTo(FIRST_KEY);
		ArgumentCaptor<SubmissionNotice> notice = ArgumentCaptor.forClass(SubmissionNotice.class);
		then(submissionNotifier).should().notifySubmitted(notice.capture());
		assertThat(notice.getValue().submissionId()).isEqualTo(submission.getId());
		assertThat(notice.getValue().userId()).isEqualTo(USER_ID);
		assertThat(notice.getValue().topicId()).isEqualTo(topic.getId());
		assertThat(notice.getValue().topicTitle()).isEqualTo("분리불안");
		assertThat(notice.getValue().attemptNo()).isEqualTo(1);
	}

	@Test
	void 검수_대기_중에_다시_제출하면_이전_제출을_교체로_남기고_2번째_제출을_만든다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		GraduationSubmission first = graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY));
		topicSeeder.flushAndClear();

		GraduationSubmission second = graduationSubmitter.submit(USER_ID, topic.getId(), request(SECOND_KEY));
		topicSeeder.flushAndClear();

		assertThat(second.getAttemptNo()).isEqualTo(2);
		List<GraduationSubmission> submissions = graduationSubmissionRepository.findAll();
		assertThat(submissions).extracting(GraduationSubmission::getId, GraduationSubmission::getResult)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(first.getId(), SubmissionResult.REPLACED),
						org.assertj.core.groups.Tuple.tuple(second.getId(), SubmissionResult.PENDING));
	}

	@Test
	void 보완_요청을_받은_뒤_다시_제출하면_제출_상태로_돌아간다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		GraduationSubmission first = graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY));
		requestRevision(first);

		GraduationSubmission second = graduationSubmitter.submit(USER_ID, topic.getId(), request(SECOND_KEY));
		topicSeeder.flushAndClear();

		assertThat(second.getAttemptNo()).isEqualTo(2);
		assertThat(topicGraduationRepository.findByUserIdAndTopicId(USER_ID, topic.getId()).orElseThrow().getStatus())
				.isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(graduationSubmissionRepository.findById(first.getId()).orElseThrow().getResult())
				.isEqualTo(SubmissionResult.REVISION_REQUESTED);
	}

	@Test
	void 승인된_수료에_다시_제출하면_예외를_던진다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		GraduationSubmission first = graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY));
		approve(first);

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(SECOND_KEY)))
				.isInstanceOf(GraduationAlreadyApprovedException.class);
	}

	@Test
	void 다른_사용자의_영상_키로_제출하면_예외를_던지고_아무것도_만들지_않는다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request("videos/graduation/43/x.mp4")))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
		assertThat(topicGraduationRepository.findByUserIdAndTopicId(USER_ID, topic.getId())).isEmpty();
		then(submissionNotifier).should(never()).notifySubmitted(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void 커리큘럼을_다_마치지_않은_토픽에_제출하면_예외를_던진다() {
		Topic topic = topicSeeder.incompleteTopic("분리불안");

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY)))
				.isInstanceOf(TopicNotCompletedException.class);
	}

	@Test
	void 같은_토픽의_첫_제출이_동시에_들어와_유니크_제약에_걸리면_충돌_예외로_바꾼다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		topicGraduationRepository.saveAndFlush(TopicGraduation.start(USER_ID, topic.getId()));
		willReturn(Optional.empty()).given(topicGraduationRepository).findForUpdate(USER_ID, topic.getId());

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY)))
				.isInstanceOf(GraduationSubmissionConflictException.class);
	}

	private GraduationSubmissionRequest request(String objectKey) {
		return new GraduationSubmissionRequest(objectKey);
	}

	private void requestRevision(GraduationSubmission submission) {
		GraduationSubmission saved = graduationSubmissionRepository.findById(submission.getId()).orElseThrow();
		TopicGraduation graduation = topicGraduationRepository.findById(saved.getGraduationId()).orElseThrow();
		saved.requestRevision("다시 찍어 주세요.", Instant.now());
		graduation.requestRevision();
		topicSeeder.flushAndClear();
	}

	private void approve(GraduationSubmission submission) {
		GraduationSubmission saved = graduationSubmissionRepository.findById(submission.getId()).orElseThrow();
		TopicGraduation graduation = topicGraduationRepository.findById(saved.getGraduationId()).orElseThrow();
		saved.approve(null, Instant.now());
		graduation.approve(Instant.now());
		topicSeeder.flushAndClear();
	}
}
