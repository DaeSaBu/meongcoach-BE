package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.ai.domain.AiAnalysisUploadCommand;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

@DataJpaTest
@Import(AiReportQueryService.class)
class AiReportQueryServiceTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	private static final String CONTENT_JSON = "{\"recommend\":[{\"topicId\":106,\"title\":\"분리불안 교육\","
			+ "\"description\":\"혼자 있는 시간을 편안하게 만드는 교육이라 도움이 돼요.\"}],"
			+ "\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"현관 앞을 서성여요.\"}],"
			+ "\"solution\":[{\"order\":1,\"title\":\"혼자 있는 연습\",\"description\":\"짧게 자리를 비워 보세요.\"}]}";

	private static final String LEGACY_CONTENT_JSON = "{\"recommend\":[{\"title\":\"분리불안 교육\","
			+ "\"description\":\"혼자 있는 시간을 편안하게 만드는 교육이라 도움이 돼요.\"}],"
			+ "\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"현관 앞을 서성여요.\"}],"
			+ "\"solution\":[]}";

	@Autowired
	private AiReportFinder aiReportFinder;

	@Autowired
	private AiAnalysisRepository aiAnalysisRepository;

	@TestConfiguration
	static class ObjectMapperTestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}
	}

	@Test
	void 내_리포트만_최신순으로_반환한다() {
		persistCompletedAnalysis(USER_ID, "videos/training/42/first.mp4", "첫 제목");
		persistCompletedAnalysis(USER_ID, "videos/training/42/second.mp4", "둘째 제목");
		persistCompletedAnalysis(OTHER_USER_ID, "videos/training/99/other.mp4", "남의 제목");

		List<AiReportResult> reports = aiReportFinder.findReports(USER_ID);

		assertThat(reports).extracting(AiReportResult::videoObjectKey)
				.containsExactly("videos/training/42/second.mp4", "videos/training/42/first.mp4");
	}

	@Test
	void 리포트가_없으면_빈_리스트를_반환한다() {
		assertThat(aiReportFinder.findReports(USER_ID)).isEmpty();
	}

	@Test
	void 목록_항목에_식별자_영상_객체_키_제목_상태_생성_시각을_담는다() {
		AiAnalysis saved = persistCompletedAnalysis(USER_ID, "videos/training/42/key.mp4", "분리불안 징후 행동 분석");

		AiReportResult result = aiReportFinder.findReports(USER_ID).getFirst();

		assertThat(result.id()).isEqualTo(saved.getId());
		assertThat(result.videoObjectKey()).isEqualTo("videos/training/42/key.mp4");
		assertThat(result.title()).isEqualTo("분리불안 징후 행동 분석");
		assertThat(result.status()).isEqualTo(AiAnalysisStatus.COMPLETED);
		assertThat(result.createdAt()).isNotNull();
	}

	@Test
	void 완료되지_않은_리포트도_목록에_상태와_함께_나온다() {
		aiAnalysisRepository.saveAndFlush(startedAnalysis(USER_ID, "videos/training/42/pending.mp4"));
		persistFailedAnalysis(USER_ID, "videos/training/42/failed.mp4");

		List<AiReportResult> reports = aiReportFinder.findReports(USER_ID);

		assertThat(reports).extracting(AiReportResult::status)
				.containsExactly(AiAnalysisStatus.FAILED_ANALYSIS, AiAnalysisStatus.ANALYZING);
		assertThat(reports).extracting(AiReportResult::title).containsOnlyNulls();
	}

	@Test
	void 제목이_없는_리포트는_목록에서_제목이_null이다() {
		persistCompletedAnalysis(USER_ID, "videos/training/42/key.mp4", null);

		AiReportResult result = aiReportFinder.findReports(USER_ID).getFirst();

		assertThat(result.title()).isNull();
	}

	@Test
	void 리포트_하나를_구조화된_본문까지_모든_필드와_함께_반환한다() {
		AiAnalysis saved = persistCompletedAnalysis(USER_ID, "videos/training/42/key.mp4", "분리불안 징후 행동 분석");

		AiReportDetailResult detail = aiReportFinder.findReport(USER_ID, saved.getId());

		assertThat(detail.id()).isEqualTo(saved.getId());
		assertThat(detail.videoObjectKey()).isEqualTo("videos/training/42/key.mp4");
		assertThat(detail.title()).isEqualTo("분리불안 징후 행동 분석");
		assertThat(detail.status()).isEqualTo(AiAnalysisStatus.COMPLETED);
		assertThat(detail.createdAt()).isNotNull();
		AiReportContent content = detail.content();
		assertThat(content.recommend()).containsExactly(new AiReportContent.Recommend(
				106L, "분리불안 교육", "혼자 있는 시간을 편안하게 만드는 교육이라 도움이 돼요."));
		assertThat(content.report()).containsExactly(
				new AiReportContent.ReportSection("영상에서 이런 행동이 보여요", "현관 앞을 서성여요."));
		assertThat(content.solution()).containsExactly(
				new AiReportContent.Solution(1, "혼자 있는 연습", "짧게 자리를 비워 보세요."));
	}

	@Test
	void topicId_도입_전에_저장된_리포트는_추천_교육의_topicId가_null이다() {
		AiAnalysis saved = persistCompletedAnalysis(USER_ID, "videos/training/42/legacy.mp4", "분리불안 징후 행동 분석",
				LEGACY_CONTENT_JSON);

		AiReportDetailResult detail = aiReportFinder.findReport(USER_ID, saved.getId());

		AiReportContent.Recommend recommend = detail.content().recommend().getFirst();
		assertThat(recommend.topicId()).isNull();
		assertThat(recommend.title()).isEqualTo("분리불안 교육");
	}

	@Test
	void 완료되지_않은_리포트_상세는_본문이_null이고_상태를_담는다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(startedAnalysis(USER_ID, "videos/training/42/pending.mp4"));

		AiReportDetailResult detail = aiReportFinder.findReport(USER_ID, saved.getId());

		assertThat(detail.status()).isEqualTo(AiAnalysisStatus.ANALYZING);
		assertThat(detail.title()).isNull();
		assertThat(detail.content()).isNull();
	}

	@Test
	void 존재하지_않는_리포트면_예외를_던진다() {
		assertThatThrownBy(() -> aiReportFinder.findReport(USER_ID, 999L))
				.isInstanceOf(AiReportNotFoundException.class);
	}

	@Test
	void 다른_사용자의_리포트면_존재해도_예외를_던진다() {
		AiAnalysis saved = persistCompletedAnalysis(OTHER_USER_ID, "videos/training/99/other.mp4", "남의 제목");

		assertThatThrownBy(() -> aiReportFinder.findReport(USER_ID, saved.getId()))
				.isInstanceOf(AiReportNotFoundException.class);
	}

	@Test
	void 업로드_만료_전의_UPLOADING_리포트는_목록에_UPLOADING으로_나온다() {
		aiAnalysisRepository.saveAndFlush(uploadingAnalysis(USER_ID, "videos/training/42/uploading.mp4",
				Instant.now().plusSeconds(900)));

		AiReportResult result = aiReportFinder.findReports(USER_ID).getFirst();

		assertThat(result.status()).isEqualTo(AiAnalysisStatus.UPLOADING);
	}

	@Test
	void 업로드가_만료된_UPLOADING_리포트는_목록에_FAILED_UPLOAD로_나온다() {
		aiAnalysisRepository.saveAndFlush(uploadingAnalysis(USER_ID, "videos/training/42/expired.mp4",
				Instant.now().minusSeconds(1)));

		AiReportResult result = aiReportFinder.findReports(USER_ID).getFirst();

		assertThat(result.status()).isEqualTo(AiAnalysisStatus.FAILED_UPLOAD);
	}

	@Test
	void 업로드가_만료된_UPLOADING_리포트_상세는_FAILED_UPLOAD와_빈_본문을_담는다() {
		AiAnalysis saved = aiAnalysisRepository.saveAndFlush(uploadingAnalysis(USER_ID, "videos/training/42/expired.mp4",
				Instant.now().minusSeconds(1)));

		AiReportDetailResult detail = aiReportFinder.findReport(USER_ID, saved.getId());

		assertThat(detail.status()).isEqualTo(AiAnalysisStatus.FAILED_UPLOAD);
		assertThat(detail.content()).isNull();
	}

	private void persistFailedAnalysis(Long userId, String videoObjectKey) {
		AiAnalysis analysis = startedAnalysis(userId, videoObjectKey);
		analysis.failByAnalysis();
		aiAnalysisRepository.saveAndFlush(analysis);
	}

	private AiAnalysis persistCompletedAnalysis(Long userId, String videoObjectKey, String title) {
		return persistCompletedAnalysis(userId, videoObjectKey, title, CONTENT_JSON);
	}

	private AiAnalysis persistCompletedAnalysis(Long userId, String videoObjectKey, String title, String contentJson) {
		AiAnalysis analysis = startedAnalysis(userId, videoObjectKey);
		analysis.complete(title, contentJson);
		return aiAnalysisRepository.saveAndFlush(analysis);
	}

	private static AiAnalysis uploadingAnalysis(Long userId, String videoObjectKey, Instant uploadExpiresAt) {
		return AiAnalysis.uploading(new AiAnalysisUploadCommand(userId, videoObjectKey, uploadExpiresAt));
	}

	private static AiAnalysis startedAnalysis(Long userId, String videoObjectKey) {
		AiAnalysis analysis = uploadingAnalysis(userId, videoObjectKey, Instant.now().plusSeconds(900));
		analysis.startAnalysis();
		return analysis;
	}
}
