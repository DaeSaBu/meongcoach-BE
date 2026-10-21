package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationAlreadyApprovedException;
import com.daesabu.meongcoach.graduation.domain.exception.TopicNotCompletedException;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
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
	private TopicGraduationRepository topicGraduationRepository;

	@Autowired
	private TopicSeeder topicSeeder;

	@Autowired
	private VideoStorage videoStorage;

	@Test
	void 토픽을_완료한_회원에게_본인_토픽_실습_영상_경로의_업로드_URL을_발급한다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		given(videoStorage.issueUploadUrl(any(), eq(REQUEST.contentType()), eq(REQUEST.fileSizeBytes())))
				.willAnswer(invocation -> {
					VideoObjectKey key = invocation.getArgument(0);
					String objectKey = key.value();
					return new VideoUploadUrl("https://storage.test/upload", "https://videos.test/" + objectKey,
							objectKey, 900L);
				});

		VideoUploadUrlResult result = graduationUploadUrlIssuer.issue(USER_ID, topic.getId(), REQUEST);

		assertThat(result.objectKey()).startsWith("videos/graduation/42/" + topic.getId() + "/").endsWith(".mp4");
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
		TopicGraduation graduation = graduationSubmitter.submit(USER_ID, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/42/" + topic.getId() + "/a.mp4"));
		graduation.approve(currentSubmission(graduation), null, Instant.now());
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
	private GraduationSubmission currentSubmission(TopicGraduation graduation) {
		return graduationSubmissionRepository
				.findFirstByGraduationIdOrderByIdDesc(graduation.getId()).orElseThrow();
	}

}
