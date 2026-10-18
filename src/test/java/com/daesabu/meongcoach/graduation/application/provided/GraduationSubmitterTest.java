package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.willReturn;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationSubmissionConflictException;
import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
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
	private TopicSeeder topicSeeder;

	@Test
	void 첫_제출은_1번째_제출로_수료_행을_만들고_기록을_남긴다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		TopicGraduation graduation = graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY));
		topicSeeder.flushAndClear();

		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(graduation.getAttemptNo()).isEqualTo(1);
		assertThat(graduation.getVideoObjectKey()).isEqualTo(FIRST_KEY);
		assertThat(graduationSubmissionRepository.findAll())
				.extracting(GraduationSubmission::getGraduationId, GraduationSubmission::getAttemptNo,
						GraduationSubmission::getVideoObjectKey)
				.containsExactly(tuple(graduation.getId(), 1, FIRST_KEY));
	}

	@Test
	void 검수_대기_중에_다시_제출하면_같은_행의_차수를_올리고_영상을_바꾸고_기록을_한_줄_더_남긴다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		TopicGraduation first = graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY));
		topicSeeder.flushAndClear();

		TopicGraduation second = graduationSubmitter.submit(USER_ID, topic.getId(), request(SECOND_KEY));
		topicSeeder.flushAndClear();

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(second.getAttemptNo()).isEqualTo(2);
		assertThat(second.getVideoObjectKey()).isEqualTo(SECOND_KEY);
		assertThat(graduationSubmissionRepository.findAll())
				.extracting(GraduationSubmission::getAttemptNo, GraduationSubmission::getVideoObjectKey)
				.containsExactlyInAnyOrder(tuple(1, FIRST_KEY), tuple(2, SECOND_KEY));
	}

	@Test
	void 보완_요청을_받은_뒤_다시_제출하면_제출_상태로_돌아가고_메모가_지워진다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		TopicGraduation first = graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY));
		first.requestRevision("다시 찍어 주세요.", Instant.now());
		topicSeeder.flushAndClear();

		TopicGraduation second = graduationSubmitter.submit(USER_ID, topic.getId(), request(SECOND_KEY));
		topicSeeder.flushAndClear();

		TopicGraduation saved = topicGraduationRepository.findById(second.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(saved.getAttemptNo()).isEqualTo(2);
		assertThat(saved.getReviewNote()).isNull();
		assertThat(saved.getReviewedAt()).isNull();
	}

	@Test
	void 승인된_수료에_다시_제출하면_예외를_던진다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		TopicGraduation first = graduationSubmitter.submit(USER_ID, topic.getId(), request(FIRST_KEY));
		first.approve(null, Instant.now(), Instant.now());
		topicSeeder.flushAndClear();

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(SECOND_KEY)))
				.isInstanceOf(GraduationAlreadyApprovedException.class);
	}

	@Test
	void 다른_사용자의_영상_키로_제출하면_예외를_던지고_아무것도_만들지_않는다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request("videos/graduation/43/x.mp4")))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
		assertThat(topicGraduationRepository.findByUserIdAndTopicId(USER_ID, topic.getId())).isEmpty();
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
		topicGraduationRepository.saveAndFlush(TopicGraduation.start(USER_ID, topic.getId(), FIRST_KEY, Instant.now()));
		willReturn(Optional.empty()).given(topicGraduationRepository).findForUpdate(USER_ID, topic.getId());

		assertThatThrownBy(() -> graduationSubmitter.submit(USER_ID, topic.getId(), request(SECOND_KEY)))
				.isInstanceOf(GraduationSubmissionConflictException.class);
	}

	private GraduationSubmissionRequest request(String objectKey) {
		return new GraduationSubmissionRequest(objectKey);
	}
}
