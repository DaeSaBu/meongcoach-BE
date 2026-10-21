package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.LessonProgresses;
import java.util.Collection;
import java.util.Map;

public interface LessonProgressFinder {

	LessonProgresses findProgresses(Long userId, Collection<Long> lessonIds);

	Map<Long, Integer> findCompletedCounts(Long userId, Collection<Long> lessonIds);
}
