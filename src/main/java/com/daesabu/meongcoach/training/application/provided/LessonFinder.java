package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.Card;
import java.util.List;

public interface LessonFinder {

	List<Card> findCards(Long lessonId);
}
