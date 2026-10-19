package com.daesabu.meongcoach.community.adapter.webapi;

import com.daesabu.meongcoach.community.adapter.webapi.dto.PostListResponse;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

	private final PostFinder postFinder;

	@GetMapping("/posts")
	public PostListResponse findPosts(@Valid @ModelAttribute PostListRequest request) {
		PostListResult result = postFinder.findPosts(request);
		return PostListResponse.from(result);
	}
}
