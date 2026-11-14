package com.daesabu.meongcoach.ai.application.required;

import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AI 분석 건 저장 리포지토리.
 */
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

	/**
	 * 영상 객체 키로 분석 건을 찾는다.
	 */
	Optional<AiAnalysis> findByVideoObjectKey(String videoObjectKey);

	/**
	 * 사용자의 분석 건을 생성 시각 내림차순으로 조회한다.
	 * 같은 시각이면 id 내림차순으로 순서를 고정한다.
	 */
	List<AiAnalysis> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

	/**
	 * 분석 건 ID와 소유자가 모두 일치할 때만 조회한다.
	 */
	Optional<AiAnalysis> findByIdAndUserId(Long id, Long userId);
}
