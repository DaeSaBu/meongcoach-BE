package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.application.provided.AiReportContent;
import com.daesabu.meongcoach.ai.application.provided.AiReportDetailResult;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportResult;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

/**
 * AI 리포트 조회 서비스 검증.
 */
@DataJpaTest
@Import(AiReportQueryService.class)
@DisplayName("AI 리포트 조회 서비스")
class AiReportQueryServiceTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	private static final String CONTENT_JSON = "{\"recommend\":[{\"title\":\"분리불안 교육\","
			+ "\"description\":\"혼자 있는 시간을 편안하게 만드는 교육이라 도움이 돼요.\"}],"
			+ "\"report\":[{\"subTitle\":\"영상에서 이런 행동이 보여요\",\"description\":\"현관 앞을 서성여요.\"}],"
			+ "\"solution\":[{\"order\":1,\"title\":\"혼자 있는 연습\",\"description\":\"짧게 자리를 비워 보세요.\"}]}";

	@Autowired
	private AiReportFinder aiReportFinder;

	@Autowired
	private AiReportRepository aiReportRepository;

	// @DataJpaTest 슬라이스에는 Jackson ObjectMapper 빈이 없어 직접 등록한다
	@TestConfiguration
	static class ObjectMapperTestConfig {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}
	}

	@Test
	@DisplayName("내 리포트만 최신순으로 반환한다")
	void findReportsReturnsOwnReportsLatestFirst() {
		persistReport(USER_ID, "videos/training/42/first.mp4", "첫 제목");
		persistReport(USER_ID, "videos/training/42/second.mp4", "둘째 제목");
		persistReport(OTHER_USER_ID, "videos/training/99/other.mp4", "남의 제목");

		List<AiReportResult> reports = aiReportFinder.findReports(USER_ID);

		// createdAt은 @PrePersist로 세팅되어 조작할 수 없으므로
		// "나중 저장 = 최신"으로 순서를 검증한다
		assertThat(reports).extracting(AiReportResult::videoObjectKey)
				.containsExactly("videos/training/42/second.mp4", "videos/training/42/first.mp4");
	}

	@Test
	@DisplayName("리포트가 없으면 빈 리스트를 반환한다")
	void findReportsReturnsEmptyListWhenNoReportExists() {
		assertThat(aiReportFinder.findReports(USER_ID)).isEmpty();
	}

	@Test
	@DisplayName("목록 항목에 식별자·영상 객체 키·제목·상태·생성 시각을 담는다")
	void findReportsMapsListFields() {
		AiReport saved = persistReport(USER_ID, "videos/training/42/key.mp4", "분리불안 징후 행동 분석");

		AiReportResult result = aiReportFinder.findReports(USER_ID).getFirst();

		assertThat(result.id()).isEqualTo(saved.getId());
		assertThat(result.videoObjectKey()).isEqualTo("videos/training/42/key.mp4");
		assertThat(result.title()).isEqualTo("분리불안 징후 행동 분석");
		assertThat(result.status()).isEqualTo(AiReportStatus.COMPLETED);
		assertThat(result.createdAt()).isNotNull();
	}

	@Test
	@DisplayName("완료되지 않은 리포트도 목록에 상태와 함께 나온다")
	void findReportsIncludesIncompleteReportsWithStatus() {
		aiReportRepository.saveAndFlush(AiReport.pending(USER_ID, "videos/training/42/pending.mp4"));
		persistFailedReport(USER_ID, "videos/training/42/failed.mp4", AiReportStatus.FAILED_ANALYSIS);

		List<AiReportResult> reports = aiReportFinder.findReports(USER_ID);

		assertThat(reports).extracting(AiReportResult::status)
				.containsExactly(AiReportStatus.FAILED_ANALYSIS, AiReportStatus.PENDING);
		assertThat(reports).extracting(AiReportResult::title).containsOnlyNulls();
	}

	@Test
	@DisplayName("제목이 없는 리포트는 목록에서 제목이 null이다")
	void findReportsAllowsNullTitle() {
		persistReport(USER_ID, "videos/training/42/key.mp4", null);

		AiReportResult result = aiReportFinder.findReports(USER_ID).getFirst();

		assertThat(result.title()).isNull();
	}

	@Test
	@DisplayName("리포트 하나를 구조화된 본문까지 모든 필드와 함께 반환한다")
	void findReportMapsAllDetailFields() {
		AiReport saved = persistReport(USER_ID, "videos/training/42/key.mp4", "분리불안 징후 행동 분석");

		AiReportDetailResult detail = aiReportFinder.findReport(USER_ID, saved.getId());

		assertThat(detail.id()).isEqualTo(saved.getId());
		assertThat(detail.videoObjectKey()).isEqualTo("videos/training/42/key.mp4");
		assertThat(detail.title()).isEqualTo("분리불안 징후 행동 분석");
		assertThat(detail.status()).isEqualTo(AiReportStatus.COMPLETED);
		assertThat(detail.createdAt()).isNotNull();
		AiReportContent content = detail.content();
		assertThat(content.recommend()).containsExactly(new AiReportContent.Recommend(
				"분리불안 교육", "혼자 있는 시간을 편안하게 만드는 교육이라 도움이 돼요."));
		assertThat(content.report()).containsExactly(
				new AiReportContent.ReportSection("영상에서 이런 행동이 보여요", "현관 앞을 서성여요."));
		assertThat(content.solution()).containsExactly(
				new AiReportContent.Solution(1, "혼자 있는 연습", "짧게 자리를 비워 보세요."));
	}

	@Test
	@DisplayName("완료되지 않은 리포트 상세는 본문이 null이고 상태를 담는다")
	void findReportReturnsNullContentForIncompleteReport() {
		AiReport saved = aiReportRepository.saveAndFlush(AiReport.pending(USER_ID, "videos/training/42/pending.mp4"));

		AiReportDetailResult detail = aiReportFinder.findReport(USER_ID, saved.getId());

		assertThat(detail.status()).isEqualTo(AiReportStatus.PENDING);
		assertThat(detail.title()).isNull();
		assertThat(detail.content()).isNull();
	}

	@Test
	@DisplayName("존재하지 않는 리포트면 예외를 던진다")
	void findReportThrowsWhenReportDoesNotExist() {
		assertThatThrownBy(() -> aiReportFinder.findReport(USER_ID, 999L))
				.isInstanceOf(AiReportNotFoundException.class);
	}

	@Test
	@DisplayName("다른 사용자의 리포트면 존재해도 예외를 던진다")
	void findReportThrowsForOtherUsersReport() {
		AiReport saved = persistReport(OTHER_USER_ID, "videos/training/99/other.mp4", "남의 제목");

		assertThatThrownBy(() -> aiReportFinder.findReport(USER_ID, saved.getId()))
				.isInstanceOf(AiReportNotFoundException.class);
	}

	private void persistFailedReport(Long userId, String videoObjectKey, AiReportStatus status) {
		AiReport report = AiReport.pending(userId, videoObjectKey);
		report.fail(status);
		aiReportRepository.saveAndFlush(report);
	}

	private AiReport persistReport(Long userId, String videoObjectKey, String title) {
		AiReport report = AiReport.pending(userId, videoObjectKey);
		report.complete(title, CONTENT_JSON);
		return aiReportRepository.saveAndFlush(report);
	}
}
