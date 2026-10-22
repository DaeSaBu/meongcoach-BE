package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CardListResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CurriculumDetailResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CurriculumListResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.LessonCompleteResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TrainingCategoryListResponse;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumDetailResult;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumListResult;
import com.daesabu.meongcoach.training.application.provided.dto.TopicSelectionRequest;
import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

	private final TrainingCategoryFinder trainingCategoryFinder;
	private final TopicSelector topicSelector;
	private final CurriculumFinder curriculumFinder;
	private final LessonFinder lessonFinder;
	private final LessonCompleter lessonCompleter;

	@GetMapping("/categories")
	public TrainingCategoryListResponse findAll() {
		List<TrainingCategory> categories = trainingCategoryFinder.findAllOrdered();
		return TrainingCategoryListResponse.from(categories);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PutMapping("/topic/selection")
	public void selectTopic(@CurrentUserId Long userId,
	                        @Valid @RequestBody TopicSelectionRequest request) {
		topicSelector.selectTopic(userId, request);
	}

	@GetMapping("/topic/selection/curriculums")
	public CurriculumListResponse findCurriculums(@CurrentUserId Long userId) {
		CurriculumListResult result = curriculumFinder.findCurriculums(userId);
		return CurriculumListResponse.from(result);
	}

	@GetMapping("/curriculums/{curriculumId}")
	public CurriculumDetailResponse findCurriculum(@CurrentUserId Long userId, @PathVariable Long curriculumId) {
		CurriculumDetailResult result = curriculumFinder.findCurriculum(userId, curriculumId);
		return CurriculumDetailResponse.from(result);
	}

	@GetMapping("/lessons/{lessonId}/cards")
	public CardListResponse findCards(@CurrentUserId Long userId, @PathVariable Long lessonId) {
		List<Card> cards = lessonFinder.findCards(userId, lessonId);
		return CardListResponse.from(cards);
	}

	@PostMapping("/lessons/{lessonId}/completion")
	@ResponseStatus(HttpStatus.CREATED)
	public LessonCompleteResponse completeLesson(@CurrentUserId Long userId, @PathVariable Long lessonId) {
		int completedCount = lessonCompleter.completeLesson(userId, lessonId);
		return new LessonCompleteResponse(lessonId, completedCount);
	}
}
