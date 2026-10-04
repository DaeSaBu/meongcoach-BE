package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonQueryService implements LessonFinder {
	private final LessonRepository lessonRepository;
	private final CardRepository cardRepository;

	@Override
	public List<Card> findCards(Long lessonId) {
		if (!lessonRepository.existsById(lessonId)) {
			throw new LessonNotFoundException(lessonId);
		}

		return cardRepository.findAllByLessonId(lessonId);
	}
}
