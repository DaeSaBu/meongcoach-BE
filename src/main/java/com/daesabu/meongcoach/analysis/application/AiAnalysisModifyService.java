package com.daesabu.meongcoach.analysis.application;

import com.daesabu.meongcoach.analysis.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.application.required.dto.VideoAnalysisResult;
import com.daesabu.meongcoach.analysis.domain.AiAnalysis;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiAnalysisModifyService {

	private final AiAnalysisRepository aiAnalysisRepository;
	private final AiTrialRepository aiTrialRepository;

	@Transactional
	public void complete(AiAnalysis analysis, VideoAnalysisResult result) {
		Long userId = analysis.getUserId();
		aiTrialRepository.lockByUserId(userId);

		AiTrial trial = aiTrialRepository.findByUserId(userId)
				.orElseGet(() -> AiTrial.start(userId));
		trial.use();
		aiTrialRepository.save(trial);

		analysis.complete(result.title(), result.content());
		aiAnalysisRepository.save(analysis);
	}
}
