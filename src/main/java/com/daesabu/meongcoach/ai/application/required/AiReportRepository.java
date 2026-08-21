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
	 * 같은 영상(S3 객체 키 기준)에 대한 리포트가 이미 있는지 확인한다.
	 * SQS 중복 전달로 인한 이중 생성을 막는 멱등 가드다.
	 * 상태를 가리지 않으므로 PENDING·FAILED row에도 걸려 같은 영상은 재분석되지 않는다 (의도).
	 */
	boolean existsByVideoObjectKey(String videoObjectKey);

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
	 * 영상 객체 키와 소유자가 모두 일치할 때만 조회한다. 상태는 가리지 않는다.
	 * 남의 영상 키는 결과 없음으로 처리해 존재 여부를 숨긴다.
	 */
	Optional<AiReport> findByVideoObjectKeyAndUserId(String videoObjectKey, Long userId);

	/**
	 * 사용자의 리포트 중 주어진 상태인 것만 센다.
	 * 무료 체험 한도는 COMPLETED만 센다 — 실패·진행 중 리포트는 체험을 소모하지 않는다.
	 */
	long countByUserIdAndStatus(Long userId, AiReportStatus status);
}
