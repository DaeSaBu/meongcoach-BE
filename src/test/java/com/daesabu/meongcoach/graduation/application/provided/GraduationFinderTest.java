package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@ApplicationTest
@Import(TopicSeeder.class)
class GraduationFinderTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 43L;

	@Autowired
	private GraduationFinder graduationFinder;

	@Autowired
	private GraduationSubmitter graduationSubmitter;

	@Autowired
	private GraduationReviewer graduationReviewer;

	@Autowired
	private TopicSeeder topicSeeder;

	@Test
	void 제출한_적이_없으면_토픽과_자격만_담아_돌려준다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		GraduationDetailResult result = graduationFinder.findGraduation(USER_ID, topic.getId());

		assertThat(result.topic().title()).isEqualTo("분리불안");
		assertThat(result.eligible()).isTrue();
		assertThat(result.graduation()).isNull();
		assertThat(result.latestSubmission()).isNull();
		assertThat(result.latestVideoUrl()).isNull();
	}

	@Test
	void 커리큘럼을_다_마치지_않았으면_자격이_없다() {
		Topic topic = topicSeeder.incompleteTopic("분리불안");

		assertThat(graduationFinder.findGraduation(USER_ID, topic.getId()).eligible()).isFalse();
	}

	@Test
	void 제출이_있으면_수료_상태와_가장_최근_제출과_영상_다운로드_URL을_돌려준다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		submit(USER_ID, topic, "first");
		submit(USER_ID, topic, "second");

		GraduationDetailResult result = graduationFinder.findGraduation(USER_ID, topic.getId());

		assertThat(result.graduation().getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(result.latestSubmission().getAttemptNo()).isEqualTo(2);
		assertThat(result.latestSubmission().getVideoObjectKey()).isEqualTo("videos/graduation/42/second.mp4");
		assertThat(result.latestVideoUrl()).contains("videos/graduation/42/second.mp4");
	}

	@Test
	void 존재하지_않는_토픽이면_예외를_던진다() {
		assertThatThrownBy(() -> graduationFinder.findGraduation(USER_ID, 999L))
				.isInstanceOf(TopicNotFoundException.class);
	}

	@Test
	void 내_수료_목록은_최근_제출_순으로_본인_것만_돌려준다() {
		Topic first = topicSeeder.completedTopic(USER_ID, "분리불안");
		Topic second = topicSeeder.completedTopic(USER_ID, "산책");
		Topic others = topicSeeder.completedTopic(OTHER_USER_ID, "배변");
		submit(USER_ID, first, "a");
		submit(USER_ID, second, "b");
		submit(OTHER_USER_ID, others, "c");
		submit(USER_ID, first, "d");

		List<GraduationSummaryResult> results = graduationFinder.findGraduations(USER_ID);

		assertThat(results).extracting(GraduationSummaryResult::topicTitle).containsExactly("분리불안", "산책");
		assertThat(results.getFirst().latestSubmission().getAttemptNo()).isEqualTo(2);
	}

	@Test
	void 승인된_토픽만_수료한_것으로_본다() {
		Topic approved = topicSeeder.completedTopic(USER_ID, "분리불안");
		Topic pending = topicSeeder.completedTopic(USER_ID, "산책");
		GraduationSubmission submission = submit(USER_ID, approved, "a");
		submit(USER_ID, pending, "b");
		graduationReviewer.review(submission.getId(), new GraduationReviewRequest("APPROVED", null));
		topicSeeder.flushAndClear();

		assertThat(graduationFinder.isApproved(USER_ID, approved.getId())).isTrue();
		assertThat(graduationFinder.isApproved(USER_ID, pending.getId())).isFalse();
		assertThat(graduationFinder.isApproved(OTHER_USER_ID, approved.getId())).isFalse();
		assertThat(graduationFinder.findApprovedTopicIds(USER_ID)).containsExactly(approved.getId());
	}

	private GraduationSubmission submit(Long userId, Topic topic, String name) {
		GraduationSubmission submission = graduationSubmitter.submit(userId, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/" + userId + "/" + name + ".mp4"));
		topicSeeder.flushAndClear();
		return submission;
	}
}
