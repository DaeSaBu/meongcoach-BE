package com.daesabu.meongcoach.community.adapter.webapi;

import com.daesabu.meongcoach.community.adapter.webapi.dto.CommentCountsResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.CommentListResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.CommentResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.LikeStateResponse;
import com.daesabu.meongcoach.community.application.provided.CardCommentCountResult;
import com.daesabu.meongcoach.community.application.provided.CardCommentCreator;
import com.daesabu.meongcoach.community.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.community.application.provided.CardCommentLiker;
import com.daesabu.meongcoach.community.application.provided.CommentPageResult;
import com.daesabu.meongcoach.community.application.provided.CommentResult;
import com.daesabu.meongcoach.community.application.provided.LikeStateResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentCreateRequest;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class CardCommentController {

	private final CardCommentFinder commentFinder;

	private final CardCommentCreator commentCreator;

	private final CardCommentLiker commentLiker;

	@GetMapping("/cards/{cardId}/comments")
	public CommentListResponse findComments(@CurrentUserId Long userId, @PathVariable Long cardId,
	                                        @RequestParam(required = false) Long cursor) {
		CommentPageResult page = commentFinder.findComments(userId, cardId, cursor);
		return CommentListResponse.from(page, userId);
	}

	@GetMapping("/comments/{commentId}/replies")
	public CommentListResponse findReplies(@CurrentUserId Long userId, @PathVariable Long commentId,
	                                       @RequestParam(required = false) Long cursor) {
		CommentPageResult page = commentFinder.findReplies(userId, commentId, cursor);
		return CommentListResponse.from(page, userId);
	}

	@GetMapping("/comments/counts")
	public CommentCountsResponse countComments(@RequestParam Set<Long> cardIds) {
		List<CardCommentCountResult> results = commentFinder.countComments(cardIds);
		return CommentCountsResponse.from(results);
	}

	@PostMapping("/cards/{cardId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse createComment(@CurrentUserId Long userId, @PathVariable Long cardId,
	                                     @RequestBody CommentCreateRequest request) {
		CommentResult result = commentCreator.createComment(userId, cardId, request);
		return CommentResponse.from(result, userId);
	}

	@PostMapping("/comments/{commentId}/replies")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse createReply(@CurrentUserId Long userId, @PathVariable Long commentId,
	                                   @RequestBody CommentCreateRequest request) {
		CommentResult result = commentCreator.createReply(userId, commentId, request);
		return CommentResponse.from(result, userId);
	}

	@PostMapping("/comments/{commentId}/like")
	public LikeStateResponse like(@CurrentUserId Long userId, @PathVariable Long commentId) {
		LikeStateResult result = commentLiker.like(userId, commentId);
		return LikeStateResponse.from(result);
	}

	@DeleteMapping("/comments/{commentId}/like")
	public LikeStateResponse unlike(@CurrentUserId Long userId, @PathVariable Long commentId) {
		LikeStateResult result = commentLiker.unlike(userId, commentId);
		return LikeStateResponse.from(result);
	}
}
