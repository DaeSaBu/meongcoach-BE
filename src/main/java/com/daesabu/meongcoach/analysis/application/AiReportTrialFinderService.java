package com.daesabu.meongcoach.analysis.application;

import com.daesabu.meongcoach.analysis.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportTrialFinderService implements AiTrialFinder {

	private final AiTrialRepository aiTrialRepository;

	@Override
	public AiTrial findTrial(Long userId) {
		return aiTrialRepository.findByUserId(userId)
				.orElseGet(() -> AiTrial.start(userId));
	}
}
