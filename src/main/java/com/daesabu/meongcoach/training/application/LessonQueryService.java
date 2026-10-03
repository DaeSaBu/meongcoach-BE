package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CardMediaResult;
import com.daesabu.meongcoach.training.application.provided.CardResult;
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
	public List<CardResult> findCards(Long lessonId) {
		if (!lessonRepository.existsById(lessonId)) {
			throw new LessonNotFoundException(lessonId);
		}

		List<Card> cards = cardRepository.findAllByLessonId(lessonId);
		return cards.stream()
				.map(this::toResult)
				.toList();
	}

	private CardResult toResult(Card card) {
		List<CardMediaResult> cardMediaResults = card.getCardMedia().stream()
				.map(media -> new CardMediaResult(media.getId(), media.getCard().getId(), media.getMediaType(),
						media.getUrl(), media.getSortOrder()))
				.toList();
		return new CardResult(card.getId(), card.getTitle(), card.getSortOrder(), card.getInstruction(), cardMediaResults);
	}
}
