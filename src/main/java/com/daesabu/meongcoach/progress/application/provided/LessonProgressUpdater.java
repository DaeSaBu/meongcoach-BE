package com.daesabu.meongcoach.progress.application.provided;

public interface LessonProgressUpdater {

	int updateCompletion(Long userId, Long lessonId);
}
