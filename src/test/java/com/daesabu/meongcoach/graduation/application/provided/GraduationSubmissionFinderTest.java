package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.application.provided.dto.AdminSubmissionResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.exception.InvalidSubmissionResultException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@ApplicationTest
@Import(TopicSeeder.class)
class GraduationSubmissionFinderTest {

	@Autowired
	private GraduationSubmissionFinder graduationSubmissionFinder;

	@Autowired
	private GraduationSubmitter graduationSubmitter;

	@Autowired
	private GraduationReviewer graduationReviewer;

	@Autowired
	private TopicSeeder topicSeeder;

	@Test
	void 검수_대기_제출을_오래된_순으로_회원과_토픽과_영상_URL을_담아_돌려준다() {
		Topic first = topicSeeder.completedTopic(42L, "분리불안");
		Topic second = topicSeeder.completedTopic(43L, "산책");
		GraduationSubmission replaced = submit(42L, first, "a");
		GraduationSubmission older = submit(43L, second, "b");
		GraduationSubmission newer = submit(42L, first, "c");

		List<AdminSubmissionResult> results = graduationSubmissionFinder.findSubmissions("PENDING");

		assertThat(results).extracting(result -> result.submission().getId())
				.containsExactly(older.getId(), newer.getId())
				.doesNotContain(replaced.getId());
		AdminSubmissionResult latest = results.getLast();
		assertThat(latest.graduation().getUserId()).isEqualTo(42L);
		assertThat(latest.graduation().getTopicId()).isEqualTo(first.getId());
		assertThat(latest.topicTitle()).isEqualTo("분리불안");
		assertThat(latest.videoUrl()).contains("videos/graduation/42/c.mp4");
	}

	@Test
	void 결과를_지정하면_그_결과의_제출만_돌려준다() {
		Topic topic = topicSeeder.completedTopic(42L, "분리불안");
		GraduationSubmission submission = submit(42L, topic, "a");
		graduationReviewer.review(submission.getId(), new GraduationReviewRequest("APPROVED", null));
		topicSeeder.flushAndClear();

		assertThat(graduationSubmissionFinder.findSubmissions("APPROVED"))
				.extracting(result -> result.submission().getId())
				.containsExactly(submission.getId());
		assertThat(graduationSubmissionFinder.findSubmissions("PENDING")).isEmpty();
	}

	@Test
	void 지원하지_않는_결과값이면_예외를_던진다() {
		assertThatThrownBy(() -> graduationSubmissionFinder.findSubmissions("UNKNOWN"))
				.isInstanceOf(InvalidSubmissionResultException.class);
	}

	private GraduationSubmission submit(Long userId, Topic topic, String name) {
		GraduationSubmission submission = graduationSubmitter.submit(userId, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/" + userId + "/" + name + ".mp4"));
		topicSeeder.flushAndClear();
		return submission;
	}
}
