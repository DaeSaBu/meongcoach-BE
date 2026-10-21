package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.LessonProgresses;
import java.util.Collection;

public interface LessonProgressFinder {

	LessonProgresses findProgresses(Long userId, Collection<Long> lessonIds);
}
