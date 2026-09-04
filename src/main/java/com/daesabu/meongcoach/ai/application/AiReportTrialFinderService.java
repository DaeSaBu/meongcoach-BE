package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiReportRepository;
import com.daesabu.meongcoach.ai.domain.AiReportStatus;
import com.daesabu.meongcoach.ai.domain.vo.AiTrial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 리포트 무료 체험 사용 현황을 조회한다.
 * 체험 현황이 필요한 모듈 내 다른 서비스도 리포지토리를 직접 세지 않고 이 빈을 거친다.
 * MVP라 별도 카운터 없이 완료된(COMPLETED) 리포트 수(count)를 사용 횟수로 쓴다. 실패·진행 중은 세지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportTrialFinderService implements AiTrialFinder {

	private final AiReportRepository aiReportRepository;

	@Override
	public AiTrial findTrial(Long userId) {
		long completedCount = aiReportRepository.countByUserIdAndStatus(userId, AiReportStatus.COMPLETED);
		return AiTrial.of(completedCount);
	}
}
