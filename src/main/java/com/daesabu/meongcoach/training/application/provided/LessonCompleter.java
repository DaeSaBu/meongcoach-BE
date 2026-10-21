package com.daesabu.meongcoach.training.application.provided;

public interface LessonCompleter {
    /**
     * 레슨의 소속과 이용권을 확인하고 완료를 기록한다.
     */
    int completeLesson(Long userId, Long lessonId);
}
