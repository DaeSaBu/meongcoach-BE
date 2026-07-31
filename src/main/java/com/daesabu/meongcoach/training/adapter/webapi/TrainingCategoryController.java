package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.training.adapter.webapi.dto.TrainingCategoryListResponse;
import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/training-categories")
@RequiredArgsConstructor
public class TrainingCategoryController {

	private final TrainingCategoryFinder trainingCategoryFinder;

	@GetMapping
	public TrainingCategoryListResponse findAll() {
		return TrainingCategoryListResponse.from(trainingCategoryFinder.findAll());
	}
}
