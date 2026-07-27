package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CardMediaView;
import com.daesabu.meongcoach.training.application.provided.CardView;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.required.CardMediaRepository;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.CardMedia;
import com.daesabu.meongcoach.training.domain.exception.LessonNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 레슨 카드 조회 서비스. 레슨 존재 확인·카드 목록·미디어 목록을 각각 한 번씩만 조회하고 메모리에서 그룹핑한다.
 */
@Service
@RequiredArgsConstructor
public class LessonQueryService implements LessonFinder {

	private final LessonRepository lessonRepository;

	private final CardRepository cardRepository;

	private final CardMediaRepository cardMediaRepository;

	@Override
	@Transactional(readOnly = true)
	public List<CardView> findCards(Long lessonId) {
		if (!lessonRepository.existsById(lessonId)) {
			throw new LessonNotFoundException(lessonId);
		}

		List<Card> cards = cardRepository.findAllByLesson_IdOrderBySortOrderAscIdAsc(lessonId);
		Map<Long, List<CardMedia>> mediaByCardId = groupMediaByCardId(cards);

		return cards.stream()
				.map(card -> toView(card, mediaByCardId.getOrDefault(card.getId(), List.of())))
				.toList();
	}

	// 정렬된 미디어를 카드 id IN 조건으로 한 번에 읽어 카드별로 나눈다. 조회 순서가 유지되므로 그룹 안의 정렬도 그대로다
	private Map<Long, List<CardMedia>> groupMediaByCardId(List<Card> cards) {
		List<Long> cardIds = cards.stream().map(Card::getId).toList();
		return cardMediaRepository.findAllByCard_IdInOrderBySortOrderAscIdAsc(cardIds).stream()
				.collect(Collectors.groupingBy(media -> media.getCard().getId()));
	}

	private CardView toView(Card card, List<CardMedia> cardMedia) {
		List<CardMediaView> cardMediaViews = cardMedia.stream()
				.map(media -> new CardMediaView(media.getId(), media.getCard().getId(), media.getMediaType(),
						media.getUrl(), media.getSortOrder()))
				.toList();
		return new CardView(card.getId(), card.getSortOrder(), card.getInstruction(), cardMediaViews);
	}
}
