package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.exception.TopicNotFoundException;
import java.time.Duration;
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
	private TopicGraduationRepository topicGraduationRepository;

	@Autowired
	private TopicSeeder topicSeeder;

	@Test
	void 토픽을_완료한_회원에게_본인_실습_영상_경로의_업로드_URL을_발급한다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		VideoUploadUrlResult result = graduationUploadUrlIssuer.issue(USER_ID, topic.getId(), REQUEST);

		assertThat(result.objectKey()).startsWith("videos/graduation/42/").endsWith(".mp4");
		assertThat(result.uploadUrl()).isNotBlank();
		assertThat(topicGraduationRepository.findByUserIdAndTopicId(USER_ID, topic.getId())).isEmpty();
	}

	@Test
	void 커리큘럼을_다_마치지_않은_토픽이면_예외를_던진다() {
		Topic topic = topicSeeder.incompleteTopic("분리불안");

		assertThatThrownBy(() -> graduationUploadUrlIssuer.issue(USER_ID, topic.getId(), REQUEST))
				.isInstanceOf(TopicNotCompletedException.class);
	}

	@Test
	void 이미_수료한_토픽이면_예외를_던진다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		TopicGraduation graduation = TopicGraduation.start(USER_ID, topic.getId(), "videos/graduation/42/a.mp4", Instant.now());
		graduation.approve(null, Instant.now(), Instant.now().plus(Duration.ofDays(3)));
		topicGraduationRepository.save(graduation);
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
