package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TopicSelectResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.TopicSelectionRequest;
import com.daesabu.meongcoach.training.application.provided.TopicSelector;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training/topic")
@RequiredArgsConstructor
public class TrainingTopicController {

	private final TopicSelector topicSelector;

	// 커리큘럼 화면에 표시할 토픽은 사용자마다 하나뿐이므로 싱글턴 리소스를 PUT으로 교체한다
	@PutMapping("/selection")
	public TopicSelectResponse selectTopic(@CurrentUserId Long userId,
	                                       @Valid @RequestBody TopicSelectionRequest request) {
		topicSelector.selectTopic(userId, request.topicId());
		return TopicSelectResponse.from(request.topicId());
	}
}
