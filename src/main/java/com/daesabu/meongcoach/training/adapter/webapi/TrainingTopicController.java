package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.webapi.LoginUser;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TopicSelectResponse;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/topics")
@RequiredArgsConstructor
public class TrainingTopicController {

	private final TopicSelector topicSelector;

	@PutMapping("/{topicId}")
	public TopicSelectResponse selectTopic(@LoginUser Long userId, @PathVariable Long topicId) {
		topicSelector.selectTopic(userId, topicId);
		return TopicSelectResponse.from(topicId);
	}
}
