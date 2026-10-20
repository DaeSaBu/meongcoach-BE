package com.daesabu.meongcoach.community.adapter.webapi;

import com.daesabu.meongcoach.community.adapter.webapi.dto.PostListResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostRegisterResponse;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

	private final PostFinder postFinder;
	private final PostRegister postRegister;

	@GetMapping("/posts")
	public PostListResponse findPosts(@Valid @ModelAttribute PostListRequest request) {
		PostListResult result = postFinder.findPosts(request);
		return PostListResponse.from(result);
	}

	@PostMapping("/posts")
	@ResponseStatus(HttpStatus.CREATED)
	public PostRegisterResponse registerPost(@CurrentUserId Long userId,
	                                         @Valid @RequestBody PostRegisterRequest request) {
		Post post = postRegister.register(userId, request);
		return PostRegisterResponse.from(post);
	}
}
