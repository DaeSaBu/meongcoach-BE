package com.daesabu.meongcoach.progress.application.provided;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface LessonProgressFinder {

	Set<Long> findCompletedLessonIds(Long userId, Collection<Long> lessonIds);

	Map<Long, Integer> findCompletedCounts(Long userId, Collection<Long> lessonIds);
}
