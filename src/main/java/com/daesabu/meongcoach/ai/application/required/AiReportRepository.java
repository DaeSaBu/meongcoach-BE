package com.daesabu.meongcoach.ai.application.required;

import com.daesabu.meongcoach.ai.domain.AiReport;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AI 리포트 저장 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface AiReportRepository extends JpaRepository<AiReport, Long> {

	/**
	 * 영상 객체 키로 업로드 URL 발급 시 만든 리포트를 찾는다.
	 * 컨슈머가 이 row를 PENDING으로 전이하며, 상태가 UPLOADING이 아니면 SQS 중복 전달로 보고 건너뛴다.
	 */
	Optional<AiReport> findByVideoObjectKey(String videoObjectKey);

	/**
	 * 사용자의 리포트를 생성 시각 내림차순으로 조회한다.
	 * 같은 시각이면 id 내림차순으로 순서를 고정한다.
	 */
	List<AiReport> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

	/**
	 * 리포트 ID와 소유자가 모두 일치할 때만 조회한다.
	 * 남의 리포트는 결과 없음으로 처리해 존재 여부를 숨긴다.
	 */
	Optional<AiReport> findByIdAndUserId(Long id, Long userId);

	/**
	 * 사용자의 리포트 중 주어진 상태인 것만 센다.
	 * 무료 체험 한도는 COMPLETED만 센다 — 실패·진행 중 리포트는 체험을 소모하지 않는다.
	 */
	long countByUserIdAndStatus(Long userId, AiReportStatus status);
}
