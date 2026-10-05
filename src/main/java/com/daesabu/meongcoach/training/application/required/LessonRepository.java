package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
}
