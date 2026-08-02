package com.daesabu.meongcoach.ai.application.required;

import com.daesabu.meongcoach.ai.domain.AiReport;
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
}
