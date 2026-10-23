package com.daesabu.meongcoach.community.adapter.webapi;

import com.daesabu.meongcoach.community.adapter.webapi.dto.CommentListResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.CommentRegisterResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostListResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostMediaUploadUrlResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostRegisterResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostUpdateResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.ReplyListResponse;
import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.CommentRegister;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.PostMediaUploadUrlIssuer;
import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostMediaUploadUrlRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostUpdateRequest;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyListResult;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
	private final CommentRegister commentRegister;
	private final CommentFinder commentFinder;
	private final PostMediaUploadUrlIssuer postMediaUploadUrlIssuer;

	@GetMapping("/posts")
	public PostListResponse findPosts(@Valid @ModelAttribute PostListRequest request) {
		PostListResult result = postFinder.findPosts(request);
		return PostListResponse.from(result);
	}

	@GetMapping("/posts/{postId}")
	public PostResponse findPost(@PathVariable Long postId) {
		PostResult result = postFinder.findPostDetail(postId);
		return PostResponse.from(result);
	}

	@PostMapping("/posts")
	@ResponseStatus(HttpStatus.CREATED)
	public PostRegisterResponse registerPost(@CurrentUserId Long userId,
	                                         @Valid @RequestBody PostRegisterRequest request) {
		Post post = postRegister.register(userId, request);
		return PostRegisterResponse.from(post);
	}

	@PutMapping("/posts/{postId}")
	public PostUpdateResponse updatePost(@CurrentUserId Long userId, @PathVariable Long postId,
	                                     @Valid @RequestBody PostUpdateRequest request) {
		Post post = postRegister.update(userId, postId, request);
		return PostUpdateResponse.from(post);
	}

	@PostMapping("/presigned-urls")
	public PostMediaUploadUrlResponse issueMediaUploadUrl(@CurrentUserId Long userId,
	                                                      @Valid @RequestBody PostMediaUploadUrlRequest request) {
		MediaUploadUrlResult result = postMediaUploadUrlIssuer.issue(userId, request);
		return PostMediaUploadUrlResponse.from(result);
	}

	@GetMapping("/posts/{postId}/comments")
	public CommentListResponse findComments(@PathVariable Long postId,
	                                        @Valid @ModelAttribute CommentListRequest request) {
		CommentListResult result = commentFinder.findComments(postId, request);
		return CommentListResponse.from(result);
	}

	@GetMapping("/comments/{commentId}/replies")
	public ReplyListResponse findReplies(@PathVariable Long commentId,
	                                     @Valid @ModelAttribute CommentListRequest request) {
		ReplyListResult result = commentFinder.findReplies(commentId, request);
		return ReplyListResponse.from(result);
	}

	@PostMapping("/posts/{postId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentRegisterResponse registerComment(@CurrentUserId Long userId, @PathVariable Long postId,
	                                               @Valid @RequestBody CommentRegisterRequest request) {
		Comment comment = commentRegister.register(userId, postId, request);
		return CommentRegisterResponse.from(comment);
	}
}
