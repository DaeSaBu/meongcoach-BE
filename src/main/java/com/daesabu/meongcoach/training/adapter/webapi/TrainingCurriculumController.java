package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.webapi.LoginUser;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CurriculumListResponse;
import com.daesabu.meongcoach.training.application.provided.CurriculumFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/curriculums")
@RequiredArgsConstructor
public class TrainingCurriculumController {

	private final CurriculumFinder curriculumFinder;

	@GetMapping
	public CurriculumListResponse findCurriculums(@LoginUser Long userId) {
		return CurriculumListResponse.from(curriculumFinder.findCurriculums(userId));
	}
}
