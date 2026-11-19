package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.training.application.provided.TrainingCalendarFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarCurriculumResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarDayResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarLessonResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarTopicResult;
import com.daesabu.meongcoach.training.application.required.CurriculumProgressRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TrainedLessonRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.CurriculumProgressStatus;
import com.daesabu.meongcoach.training.domain.CurriculumProgresses;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TrainedLesson;
import com.daesabu.meongcoach.training.domain.TrainedLessons;
import com.daesabu.meongcoach.training.domain.TrainingCalendarPeriod;
import com.daesabu.meongcoach.training.domain.TrainingSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingCalendarQueryService implements TrainingCalendarFinder {

	private final DogProfileFinder dogProfileFinder;
	private final TrainedLessonRepository trainedLessonRepository;
	private final CurriculumProgressRepository curriculumProgressRepository;
	private final LessonRepository lessonRepository;

	@Override
	public TrainingCalendarResult find(Long userId, Long dogId, LocalDate from, LocalDate to) {
		dogProfileFinder.findDogProfile(userId, dogId);
		TrainingCalendarPeriod period = new TrainingCalendarPeriod(from, to);
		LocalDate firstTrainedDate = trainedLessonRepository
				.findFirstByUserIdAndDogIdOrderByTrainedDateAsc(userId, dogId)
				.map(TrainedLesson::getTrainedDate)
				.orElse(null);
		List<TrainedLesson> records = trainedLessonRepository.findAllByUserIdAndDogIdAndTrainedDateBetween(userId,
				dogId, from, to);
		if (records.isEmpty()) {
			return new TrainingCalendarResult(firstTrainedDate, List.of());
		}

		Set<Long> lessonIds = records.stream()
				.map(TrainedLesson::getLessonId)
				.collect(Collectors.toSet());
		List<TrainedLesson> earlierRecords = trainedLessonRepository
				.findAllByUserIdAndDogIdAndLessonIdInAndTrainedDateBefore(userId, dogId, lessonIds, from);
		List<TrainedLesson> allRecords = new ArrayList<>(records);
		allRecords.addAll(earlierRecords);
		TrainedLessons trainedLessons = new TrainedLessons(allRecords);
		List<Lesson> lessons = lessonRepository.findAllById(lessonIds).stream()
				.sorted(Lesson.trainingOrder())
				.toList();
		List<CurriculumProgress> progresses = curriculumProgressRepository
				.findAllByUserIdAndDogIdAndTrainedDateBetween(userId, dogId, from, to);
		CurriculumProgresses curriculumProgresses = new CurriculumProgresses(progresses);

		List<TrainingCalendarDayResult> days = trainedLessons.findTrainedDatesIn(period).stream()
				.map(trainedDate -> toDay(trainedDate, lessons, trainedLessons, curriculumProgresses))
				.filter(day -> !day.plan().isEmpty() || !day.library().isEmpty())
				.toList();
		return new TrainingCalendarResult(firstTrainedDate, days);
	}

	private TrainingCalendarDayResult toDay(LocalDate trainedDate, List<Lesson> lessons,
			TrainedLessons trainedLessons, CurriculumProgresses curriculumProgresses) {
		List<TrainingCalendarTopicResult> plan = toTopics(trainedDate, TrainingSource.PLAN, lessons, trainedLessons,
				curriculumProgresses);
		List<TrainingCalendarTopicResult> library = toTopics(trainedDate, TrainingSource.LIBRARY, lessons,
				trainedLessons, curriculumProgresses);
		return new TrainingCalendarDayResult(trainedDate, plan, library);
	}

	private List<TrainingCalendarTopicResult> toTopics(LocalDate trainedDate, TrainingSource source,
			List<Lesson> lessons, TrainedLessons trainedLessons, CurriculumProgresses curriculumProgresses) {
		Set<Long> lessonIds = trainedLessons.findLessonIdsOn(trainedDate, source);
		Map<Topic, Map<Curriculum, List<TrainingCalendarLessonResult>>> lessonsByTopic = new LinkedHashMap<>();
		for (Lesson lesson : lessons) {
			if (!lessonIds.contains(lesson.getId())) {
				continue;
			}
			Curriculum curriculum = lesson.getCurriculum();
			int trainedDays = trainedLessons.countTrainedDaysUntil(lesson.getId(), trainedDate);
			lessonsByTopic.computeIfAbsent(curriculum.getTopic(), topic -> new LinkedHashMap<>())
					.computeIfAbsent(curriculum, key -> new ArrayList<>())
					.add(new TrainingCalendarLessonResult(lesson, trainedDays));
		}
		return lessonsByTopic.entrySet().stream()
				.map(entry -> toTopic(entry.getKey(), entry.getValue(), trainedDate, curriculumProgresses))
				.toList();
	}

	private TrainingCalendarTopicResult toTopic(Topic topic,
			Map<Curriculum, List<TrainingCalendarLessonResult>> lessonsByCurriculum, LocalDate trainedDate,
			CurriculumProgresses curriculumProgresses) {
		List<TrainingCalendarCurriculumResult> curriculums = lessonsByCurriculum.entrySet().stream()
				.map(entry -> toCurriculum(entry.getKey(), entry.getValue(), trainedDate, curriculumProgresses))
				.toList();
		return new TrainingCalendarTopicResult(topic, curriculums);
	}

	private TrainingCalendarCurriculumResult toCurriculum(Curriculum curriculum,
			List<TrainingCalendarLessonResult> lessons, LocalDate trainedDate,
			CurriculumProgresses curriculumProgresses) {
		CurriculumProgressStatus status = curriculumProgresses.findStatus(curriculum.getId(), trainedDate);
		return new TrainingCalendarCurriculumResult(curriculum, status, lessons);
	}
}
