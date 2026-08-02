package com.daesabu.meongcoach.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.application.provided.AiReportDetailView;
import com.daesabu.meongcoach.ai.application.provided.AiReportFinder;
import com.daesabu.meongcoach.ai.application.provided.AiReportView;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportCreateCommand;
import com.daesabu.meongcoach.ai.domain.exception.AiReportNotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * AI 리포트 조회 서비스 검증.
 */
@DataJpaTest
@Import(AiReportQueryService.class)
@DisplayName("AI 리포트 조회 서비스")
class AiReportQueryServiceTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private AiReportFinder aiReportFinder;

	@Autowired
	private AiReportRepository aiReportRepository;

	@Test
	@DisplayName("내 리포트만 최신순으로 반환한다")
	void findReportsReturnsOwnReportsLatestFirst() {
		persistReport(USER_ID, "videos/training/42/first.mp4", "첫 리포트");
		persistReport(USER_ID, "videos/training/42/second.mp4", "둘째 리포트");
		persistReport(OTHER_USER_ID, "videos/training/99/other.mp4", "남의 리포트");

		List<AiReportView> reports = aiReportFinder.findReports(USER_ID);

		// createdAt은 @PrePersist로 세팅되어 조작할 수 없으므로
		// "나중 저장 = 최신"으로 순서를 검증한다
		assertThat(reports).extracting(AiReportView::videoObjectKey)
				.containsExactly("videos/training/42/second.mp4", "videos/training/42/first.mp4");
	}

	@Test
	@DisplayName("리포트가 없으면 빈 리스트를 반환한다")
	void findReportsReturnsEmptyListWhenNoReportExists() {
		assertThat(aiReportFinder.findReports(USER_ID)).isEmpty();
	}

	@Test
	@DisplayName("목록 항목에 식별자·영상 객체 키·생성 시각을 담는다")
	void findReportsMapsListFields() {
		AiReport saved = persistReport(USER_ID, "videos/training/42/key.mp4", "분리불안 징후가 관찰됩니다.");

		AiReportView view = aiReportFinder.findReports(USER_ID).getFirst();

		assertThat(view.id()).isEqualTo(saved.getId());
		assertThat(view.videoObjectKey()).isEqualTo("videos/training/42/key.mp4");
		assertThat(view.createdAt()).isNotNull();
	}

	@Test
	@DisplayName("리포트 하나를 본문까지 모든 필드와 함께 반환한다")
	void findReportMapsAllDetailFields() {
		AiReport saved = persistReport(USER_ID, "videos/training/42/key.mp4", "분리불안 징후가 관찰됩니다.");

		AiReportDetailView detail = aiReportFinder.findReport(USER_ID, saved.getId());

		assertThat(detail.id()).isEqualTo(saved.getId());
		assertThat(detail.videoObjectKey()).isEqualTo("videos/training/42/key.mp4");
		assertThat(detail.content()).isEqualTo("분리불안 징후가 관찰됩니다.");
		assertThat(detail.createdAt()).isNotNull();
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
		AiReport saved = persistReport(OTHER_USER_ID, "videos/training/99/other.mp4", "남의 리포트");

		assertThatThrownBy(() -> aiReportFinder.findReport(USER_ID, saved.getId()))
				.isInstanceOf(AiReportNotFoundException.class);
	}

	private AiReport persistReport(Long userId, String videoObjectKey, String content) {
		return aiReportRepository.saveAndFlush(AiReport.create(new AiReportCreateCommand(userId, videoObjectKey, content)));
	}
}
