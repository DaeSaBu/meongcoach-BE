package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.AiReportGenerator;
import com.daesabu.meongcoach.ai.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.ai.application.required.AiAnalysisRepository;
import com.daesabu.meongcoach.ai.application.required.VideoAnalyzer;
import com.daesabu.meongcoach.ai.application.required.dto.VideoAnalysisResult;
import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import com.daesabu.meongcoach.ai.domain.AiAnalysisCompletedEvent;
import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import com.daesabu.meongcoach.ai.domain.AiTrial;
import com.daesabu.meongcoach.ai.domain.AiVideo;
import com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.media.application.provided.MediaStorageUriResolver;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportGenerateService implements AiReportGenerator {

	private final MediaStorageUriResolver mediaStorageUriResolver;
	private final VideoAnalyzer videoAnalyzer;
	private final AiAnalysisRepository aiAnalysisRepository;
	private final AiTrialFinder aiTrialFinder;
	private final DogProfileFinder dogProfileFinder;
	private final ApplicationEventPublisher events;

	@Override
	public void generate(String objectKey) {
		AiAnalysis analysis = aiAnalysisRepository.findByVideoObjectKey(objectKey).orElse(null);
		if (analysis == null) {
			log.warn("객체 키에 대응하는 AiAnalysis가 없다: {}", objectKey);
			return;
		}
		if (!analysis.isUploading()) {
			return;
		}

		String videoUri = mediaStorageUriResolver.resolve(AiVideo.SCOPE, objectKey);

		analysis.startAnalysis();
		aiAnalysisRepository.save(analysis);

		try {
			recordOutcome(analysis, videoUri);
		}
		catch (Exception e) {
			log.error("예상하지 못한 오류로 리포트 생성에 실패했다: {}", objectKey, e);
			recordFailure(analysis, AiAnalysis::failUnexpectedly);
			return;
		}
		if (analysis.getStatus() == AiAnalysisStatus.COMPLETED) {
			events.publishEvent(new AiAnalysisCompletedEvent(analysis.getUserId()));
		}
	}

	private void recordOutcome(AiAnalysis analysis, String videoUri) {
		if (isTrialExhausted(analysis.getUserId())) {
			recordFailure(analysis, AiAnalysis::failByTrialExceeded);
			return;
		}

		DogProfileResult dog = dogProfileFinder.findSelectedDogProfile(analysis.getUserId());
		VideoAnalysisResult result;
		try {
			result = videoAnalyzer.analyze(videoUri, analysis.getContext(), dog);
		}
		catch (VideoAnalysisFailedException e) {
			log.error("영상 분석에 실패해 리포트 생성을 건너뛴다: {}", analysis.getVideoObjectKey(), e);
			recordFailure(analysis, AiAnalysis::failByAnalysis);
			return;
		}

		recordCompletion(analysis, result);
	}

	private boolean isTrialExhausted(Long userId) {
		AiTrial trial = aiTrialFinder.findTrial(userId);
		return !trial.isAvailable();
	}

	private void recordCompletion(AiAnalysis analysis, VideoAnalysisResult result) {
		analysis.complete(result.title(), result.content());
		aiAnalysisRepository.save(analysis);
	}

	private void recordFailure(AiAnalysis analysis, Consumer<AiAnalysis> transition) {
		transition.accept(analysis);
		aiAnalysisRepository.save(analysis);
	}
}
