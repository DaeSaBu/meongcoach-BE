package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
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
	private TopicSeeder topicSeeder;

	@Test
	void 제출한_적이_없으면_토픽과_자격만_담아_돌려준다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		GraduationDetailResult result = graduationFinder.findGraduation(USER_ID, topic.getId());

		assertThat(result.topic().title()).isEqualTo("분리불안");
		assertThat(result.eligible()).isTrue();
		assertThat(result.graduation()).isNull();
		assertThat(result.videoUrl()).isNull();
	}

	@Test
	void 커리큘럼을_다_마치지_않았으면_자격이_없다() {
		Topic topic = topicSeeder.incompleteTopic("분리불안");

		assertThat(graduationFinder.findGraduation(USER_ID, topic.getId()).eligible()).isFalse();
	}

	@Test
	void 제출이_있으면_수료_상태와_가장_최근_영상의_다운로드_URL을_돌려준다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		submit(USER_ID, topic, "first");
		submit(USER_ID, topic, "second");

		GraduationDetailResult result = graduationFinder.findGraduation(USER_ID, topic.getId());

		assertThat(result.graduation().getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(result.graduation().getAttemptNo()).isEqualTo(2);
		assertThat(result.videoUrl()).contains("videos/graduation/42/second.mp4");
	}

	@Test
	void 다른_회원의_제출은_보이지_않는다() {
		Topic topic = topicSeeder.completedTopic(OTHER_USER_ID, "분리불안");
		submit(OTHER_USER_ID, topic, "a");

		assertThat(graduationFinder.findGraduation(USER_ID, topic.getId()).graduation()).isNull();
	}

	@Test
	void 존재하지_않는_토픽이면_예외를_던진다() {
		assertThatThrownBy(() -> graduationFinder.findGraduation(USER_ID, 999L))
				.isInstanceOf(TopicNotFoundException.class);
	}

	private void submit(Long userId, Topic topic, String name) {
		graduationSubmitter.submit(userId, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/" + userId + "/" + name + ".mp4"));
		topicSeeder.flushAndClear();
	}
}
