package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.webapi.LoginUser;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CardListResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.LessonCompleteResponse;
import com.daesabu.meongcoach.training.application.provided.LessonCompleter;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/lessons")
@RequiredArgsConstructor
public class TrainingLessonController {

	private final LessonFinder lessonFinder;

	private final LessonCompleter lessonCompleter;

	@GetMapping("/{lessonId}")
	public CardListResponse findCards(@PathVariable Long lessonId) {
		return CardListResponse.from(lessonFinder.findCards(lessonId));
	}

	@PostMapping("/{lessonId}")
	public LessonCompleteResponse completeLesson(@LoginUser Long userId, @PathVariable Long lessonId) {
		return LessonCompleteResponse.from(lessonId, lessonCompleter.completeLesson(userId, lessonId));
	}
}
