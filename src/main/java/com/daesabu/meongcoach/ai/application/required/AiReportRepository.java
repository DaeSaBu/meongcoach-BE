package com.daesabu.meongcoach.ai.application.required;

import com.daesabu.meongcoach.ai.domain.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AI 리포트 저장 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface AiReportRepository extends JpaRepository<AiReport, Long> {

	/**
	 * 같은 영상에 대한 리포트가 이미 있는지 확인한다. SQS 중복 전달로 인한 이중 생성을 막는 멱등 가드다.
	 */
	boolean existsByVideoUrl(String videoUrl);
}
