package com.daesabu.meongcoach.training.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TrainedLessons {

	private final List<TrainedLesson> trainedLessons;

	public TrainedLessons(List<TrainedLesson> trainedLessons) {
		this.trainedLessons = List.copyOf(trainedLessons);
	}

	public List<LocalDate> findTrainedDatesIn(TrainingCalendarPeriod period) {
		return trainedLessons.stream()
				.map(TrainedLesson::getTrainedDate)
				.filter(period::contains)
				.distinct()
				.sorted()
				.toList();
	}

	public Set<Long> findLessonIdsOn(LocalDate trainedDate, TrainingSource source) {
		return trainedLessons.stream()
				.filter(trainedLesson -> trainedLesson.getTrainedDate().equals(trainedDate))
				.filter(trainedLesson -> trainedLesson.getSource() == source)
				.map(TrainedLesson::getLessonId)
				.collect(Collectors.toUnmodifiableSet());
	}

	public int countTrainedDaysUntil(Long lessonId, LocalDate trainedDate) {
		return (int) trainedLessons.stream()
				.filter(trainedLesson -> trainedLesson.getLessonId().equals(lessonId))
				.map(TrainedLesson::getTrainedDate)
				.filter(date -> !date.isAfter(trainedDate))
				.distinct()
				.count();
	}
}
