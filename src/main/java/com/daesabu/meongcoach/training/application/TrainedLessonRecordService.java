package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.required.CurriculumProgressRepository;
import com.daesabu.meongcoach.training.application.required.TrainedLessonRepository;
import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.CurriculumProgressRegisterCommand;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.TrainedLesson;
import com.daesabu.meongcoach.training.domain.TrainedLessonRecordCommand;
import com.daesabu.meongcoach.training.domain.TrainingSource;
import com.daesabu.meongcoach.training.domain.exception.LessonCompletionConflictException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class TrainedLessonRecordService {

	private final TrainedLessonRepository trainedLessonRepository;
	private final CurriculumProgressRepository curriculumProgressRepository;

	@Transactional
	public void record(Long userId, Long dogId, Lesson lesson, TrainingSource source) {
		TrainedLessonRecordCommand command = new TrainedLessonRecordCommand(dogId, lesson.getId(), source,
				Instant.now());
		TrainedLesson trainedLesson = TrainedLesson.record(userId, command);
		CurriculumProgressRegisterCommand progressCommand = new CurriculumProgressRegisterCommand(dogId,
				lesson.getCurriculum().getId(), trainedLesson.getTrainedDate());
		CurriculumProgress curriculumProgress = CurriculumProgress.register(userId, progressCommand);
		try {
			saveIfAbsent(trainedLesson);
			saveIfAbsent(curriculumProgress);
		} catch (DataIntegrityViolationException exception) {
			throw new LessonCompletionConflictException(lesson.getId(), exception);
		}
	}

	private void saveIfAbsent(TrainedLesson trainedLesson) {
		boolean recorded = trainedLessonRepository.existsByUserIdAndDogIdAndLessonIdAndTrainedDateAndSource(
				trainedLesson.getUserId(), trainedLesson.getDogId(), trainedLesson.getLessonId(),
				trainedLesson.getTrainedDate(), trainedLesson.getSource());
		if (recorded) {
			return;
		}
		trainedLessonRepository.saveAndFlush(trainedLesson);
	}

	private void saveIfAbsent(CurriculumProgress curriculumProgress) {
		boolean registered = curriculumProgressRepository.existsByUserIdAndDogIdAndCurriculumIdAndTrainedDate(
				curriculumProgress.getUserId(), curriculumProgress.getDogId(), curriculumProgress.getCurriculumId(),
				curriculumProgress.getTrainedDate());
		if (registered) {
			return;
		}
		curriculumProgressRepository.saveAndFlush(curriculumProgress);
	}
}
