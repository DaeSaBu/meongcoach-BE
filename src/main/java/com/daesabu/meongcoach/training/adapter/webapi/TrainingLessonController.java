package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.training.adapter.webapi.dto.CardListResponse;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/lessons")
@RequiredArgsConstructor
public class TrainingLessonController {

	private final LessonFinder lessonFinder;

	@GetMapping("/{lessonId}")
	public CardListResponse findCards(@PathVariable Long lessonId) {
		return CardListResponse.from(lessonFinder.findCards(lessonId));
	}
}
