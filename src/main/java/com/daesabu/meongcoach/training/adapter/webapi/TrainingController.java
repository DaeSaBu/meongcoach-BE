package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CardListResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CurriculumDetailResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CurriculumListResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.LessonCompleteResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TopicSelectResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TopicSelectionRequest;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TrainingCategoryListResponse;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import jakarta.validation.Valid;
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
		return TrainingCategoryListResponse.from(trainingCategoryFinder.findAllWithTopics());
	}

	@PutMapping("/topic/selection")
	public TopicSelectResponse selectTopic(@CurrentUserId Long userId,
	                                       @Valid @RequestBody TopicSelectionRequest request) {
		topicSelector.selectTopic(userId, request.topicId());
		return TopicSelectResponse.from(request.topicId());
	}

	@GetMapping("/topic/selection/curriculums")
	public CurriculumListResponse findCurriculums(@CurrentUserId Long userId) {
		return CurriculumListResponse.from(curriculumFinder.findCurriculums(userId));
	}

	@GetMapping("/curriculums/{curriculumId}")
	public CurriculumDetailResponse findCurriculum(@CurrentUserId Long userId, @PathVariable Long curriculumId) {
		return CurriculumDetailResponse.from(curriculumFinder.findCurriculum(userId, curriculumId));
	}

	@GetMapping("/lessons/{lessonId}/cards")
	public CardListResponse findCards(@PathVariable Long lessonId) {
		return CardListResponse.from(lessonFinder.findCards(lessonId));
	}

	@PostMapping("/lessons/{lessonId}/completion")
	@ResponseStatus(HttpStatus.CREATED)
	public LessonCompleteResponse completeLesson(@CurrentUserId Long userId, @PathVariable Long lessonId) {
		return LessonCompleteResponse.from(lessonId, lessonCompleter.completeLesson(userId, lessonId));
	}
}
