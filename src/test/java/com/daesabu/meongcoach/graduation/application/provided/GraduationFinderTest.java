package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationDetailResult;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSummaryResult;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.media.application.required.VideoDownloadUrl;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

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

	@Autowired
	private VideoStorage videoStorage;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
		String objectKey = "videos/graduation/42/" + topic.getId() + "/second.mp4";
		given(videoStorage.issueDownloadUrl(new VideoObjectKey(objectKey)))
				.willReturn(new VideoDownloadUrl("https://storage.test/" + objectKey,
						"https://videos.test/" + objectKey, 900L));

		GraduationDetailResult result = graduationFinder.findGraduation(USER_ID, topic.getId());

		assertThat(result.graduation().getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(result.graduation().getAttemptNo()).isEqualTo(1);
		assertThat(result.videoUrl()).contains("videos/graduation/42/" + topic.getId() + "/second.mp4");
	}

	@Test
	void 운영자가_남긴_JSON_재촬영_지시를_순서대로_돌려준다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		submit(USER_ID, topic, "first");
		Long graduationId = jdbcTemplate.queryForObject(
				"select id from topic_graduations where user_id = ? and topic_id = ?", Long.class, USER_ID, topic.getId());
		jdbcTemplate.update("update graduation_submissions set status = 'REVISION_REQUESTED', review_note = '다시 찍어 주세요.', "
				+ "reviewed_at = now(), retake_image_url = 'https://images.test/images/graduation-retake/3/example.jpg', "
				+ "retake_instructions = '[\"첫째 지시\", \"둘째 지시\", \"셋째 지시\"]'::jsonb "
				+ "where id = (select current_submission_id from topic_graduations where id = ?)", graduationId);
		jdbcTemplate.update("update topic_graduations set status = 'REVISION_REQUESTED' where id = ?", graduationId);
		String objectKey = "videos/graduation/42/" + topic.getId() + "/first.mp4";
		given(videoStorage.issueDownloadUrl(new VideoObjectKey(objectKey)))
				.willReturn(new VideoDownloadUrl("https://storage.test/" + objectKey,
						"https://videos.test/" + objectKey, 900L));

		GraduationDetailResult result = graduationFinder.findGraduation(USER_ID, topic.getId());

		assertThat(result.graduation().getRetakeImageUrl()).isEqualTo("https://images.test/images/graduation-retake/3/example.jpg");
		assertThat(result.graduation().getRetakeInstructions()).containsExactly("첫째 지시", "둘째 지시", "셋째 지시");
	}

	@Test
	void 재제출하면_새_차수는_빈_안내를_갖고_이전_차수의_JSON_안내는_유지한다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		submit(USER_ID, topic, "first");
		jdbcTemplate.update("update graduation_submissions set status = 'REVISION_REQUESTED', "
				+ "retake_image_url = 'https://images.test/images/graduation-retake/3/example.jpg', retake_instructions = '[\"다시 찍어 주세요.\"]'::jsonb "
				+ "where id = (select current_submission_id from topic_graduations where user_id = ? and topic_id = ?)", USER_ID, topic.getId());
		jdbcTemplate.update("update topic_graduations set status = 'REVISION_REQUESTED' "
				+ "where user_id = ? and topic_id = ?", USER_ID, topic.getId());

		submit(USER_ID, topic, "second");

		assertThat(jdbcTemplate.queryForObject("select s.retake_instructions::text from topic_graduations g "
				+ "join graduation_submissions s on s.id = g.current_submission_id "
				+ "where user_id = ? and topic_id = ?", String.class, USER_ID, topic.getId())).isEqualTo("[]");
		assertThat(jdbcTemplate.queryForObject("select s.retake_image_url from topic_graduations g "
				+ "join graduation_submissions s on s.id = g.current_submission_id "
				+ "where user_id = ? and topic_id = ?", String.class, USER_ID, topic.getId())).isNull();
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

	@Test
	void 내_훈련_영상_목록은_최근_제출_순으로_본인_것만_돌려준다() {
		Topic first = topicSeeder.completedTopic(USER_ID, "분리불안");
		Topic second = topicSeeder.completedTopic(USER_ID, "산책 줄 당김");
		Topic other = topicSeeder.completedTopic(OTHER_USER_ID, "기본 예절");
		submit(USER_ID, first, "a");
		submit(USER_ID, second, "b");
		submit(OTHER_USER_ID, other, "c");

		List<GraduationSummaryResult> results = graduationFinder.findGraduations(USER_ID);

		assertThat(results).extracting(result -> result.graduation().getTopicId())
				.containsExactly(second.getId(), first.getId());
		assertThat(results).extracting(result -> result.topic().title()).containsExactly("산책 줄 당김", "분리불안");
		assertThat(results.getFirst().graduation().getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(results).allSatisfy(result ->
				assertThat(Hibernate.isInitialized(result.graduation().getCurrentSubmission())).isTrue());
	}

	@Test
	void 제출한_적이_없으면_빈_목록이다() {
		assertThat(graduationFinder.findGraduations(USER_ID)).isEmpty();
	}

	private void submit(Long userId, Topic topic, String name) {
		graduationSubmitter.submit(userId, topic.getId(),
				new GraduationSubmissionRequest(
						"videos/graduation/" + userId + "/" + topic.getId() + "/" + name + ".mp4"));
		topicSeeder.flushAndClear();
	}
}
