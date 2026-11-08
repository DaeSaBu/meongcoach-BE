package com.daesabu.meongcoach.home.adapter.webapi;

import com.daesabu.meongcoach.home.adapter.webapi.dto.QuickTopicsResponse;
import com.daesabu.meongcoach.home.application.provided.QuickTopicFinder;
import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

	private final QuickTopicFinder quickTopicFinder;

	@GetMapping("/quick-topics")
	public QuickTopicsResponse findQuickTopics(@CurrentUserId Long userId, @RequestParam Long dogId) {
		List<QuickTopicResult> topics = quickTopicFinder.find(userId, dogId);
		return QuickTopicsResponse.from(topics);
	}
}
