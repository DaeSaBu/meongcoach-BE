package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationReviewPendingException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@ApplicationTest
@Import(TopicSeeder.class)
class GraduationUploadUrlIssuerTest {

	private static final Long USER_ID = 42L;
	private static final GraduationUploadUrlRequest REQUEST = new GraduationUploadUrlRequest("video/mp4", 1024L);

	@Autowired
	private GraduationUploadUrlIssuer graduationUploadUrlIssuer;

	@Autowired
	private GraduationSubmitter graduationSubmitter;


	@Autowired
	private GraduationSubmissionRepository graduationSubmissionRepository;

	@Autowired
	private TopicSeeder topicSeeder;

	@Test
	void 토픽을_완료한_회원에게_본인_토픽_실습_영상_경로의_업로드_URL을_발급한다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		MediaUploadUrlResult result = graduationUploadUrlIssuer.issue(USER_ID, topic.getId(), REQUEST);

		assertThat(result.objectKey()).startsWith("videos/graduation/42/" + topic.getId() + "/").endsWith(".mp4");
		assertThat(result.uploadUrl()).isNotBlank();
		assertThat(graduationSubmissionRepository.findFirstByUserIdAndTopicIdOrderByIdDesc(USER_ID,
				topic.getId())).isEmpty();
	}

	@Test
	void 커리큘럼을_다_마치지_않은_토픽이면_예외를_던진다() {
		Topic topic = topicSeeder.incompleteTopic("분리불안");

		assertThatThrownBy(() -> graduationUploadUrlIssuer.issue(USER_ID, topic.getId(), REQUEST))
				.isInstanceOf(TopicNotCompletedException.class);
	}

	@Test
	void 검수_대기_중에는_URL_발급을_거절한다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		graduationSubmitter.submit(USER_ID, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/42/" + topic.getId() + "/a.mp4"));
		assertThatThrownBy(() -> graduationUploadUrlIssuer.issue(USER_ID, topic.getId(), REQUEST)).isInstanceOf(
				GraduationReviewPendingException.class);
	}

	@Test
	void 이미_수료한_토픽이면_예외를_던진다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		GraduationSubmission graduation = graduationSubmitter.submit(USER_ID, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/42/" + topic.getId() + "/a.mp4"));
		graduation.approve(null, Instant.now());
		graduationSubmissionRepository.save(graduation);
		topicSeeder.flushAndClear();

		assertThatThrownBy(() -> graduationUploadUrlIssuer.issue(USER_ID, topic.getId(), REQUEST))
				.isInstanceOf(GraduationAlreadyApprovedException.class);
	}

	@Test
	void 존재하지_않는_토픽이면_예외를_던진다() {
		assertThatThrownBy(() -> graduationUploadUrlIssuer.issue(USER_ID, 999L, REQUEST))
				.isInstanceOf(TopicNotFoundException.class);
	}
}
