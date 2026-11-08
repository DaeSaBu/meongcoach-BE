package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.ai.domain.AiTrial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportTrialFinderService implements AiTrialFinder {

	private final AiAnalysisRepository aiAnalysisRepository;

	@Override
	public AiTrial findTrial(Long userId) {
		long completedCount = aiAnalysisRepository.countByUserIdAndStatus(userId, AiAnalysisStatus.COMPLETED);
		return AiTrial.of(completedCount);
	}
}
