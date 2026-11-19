package com.daesabu.meongcoach.analysis.application;

import com.daesabu.meongcoach.analysis.application.provided.AiTrialRegister;
import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiTrialModifyService implements AiTrialRegister {

	private final AiTrialRepository aiTrialRepository;

	@Override
	@Transactional
	public void grant(Long userId, int count) {
		aiTrialRepository.lockByUserId(userId);

		AiTrial trial = aiTrialRepository.findByUserId(userId)
				.orElseGet(() -> AiTrial.start(userId));
		trial.grant(count);
		aiTrialRepository.save(trial);
	}
}
