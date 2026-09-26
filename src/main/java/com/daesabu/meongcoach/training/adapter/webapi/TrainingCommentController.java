package com.daesabu.meongcoach.training.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CommentCountResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CommentLikesResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CommentListResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.CommentResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.LikeStateResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.ReplyListResponse;
import com.daesabu.meongcoach.training.adapter.webapi.dto.ReplyResponse;
import com.daesabu.meongcoach.training.application.provided.CardCommentCreator;
import com.daesabu.meongcoach.training.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.training.application.provided.CardCommentLikeFinder;
import com.daesabu.meongcoach.training.application.provided.CardCommentLikeUpdater;
import com.daesabu.meongcoach.training.application.provided.CommentLikeResult;
import com.daesabu.meongcoach.training.application.provided.CommentResult;
import com.daesabu.meongcoach.training.application.provided.LikeStateResult;
import com.daesabu.meongcoach.training.application.provided.ReplyResult;
import com.daesabu.meongcoach.training.application.provided.dto.CommentCreateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingCommentController {

	private final CardCommentFinder commentFinder;

	private final CardCommentCreator commentCreator;

	private final CardCommentLikeFinder likeFinder;

	private final CardCommentLikeUpdater likeUpdater;

	@GetMapping("/cards/{cardId}/comments")
	public CommentListResponse findComments(@PathVariable Long cardId) {
		List<CommentResult> results = commentFinder.findComments(cardId);
		return CommentListResponse.from(results);
	}

	@GetMapping("/cards/{cardId}/comments/count")
	public CommentCountResponse countComments(@PathVariable Long cardId) {
		long totalCount = commentFinder.countComments(cardId);
		return new CommentCountResponse(totalCount);
	}

	@GetMapping("/comments/{parentId}/replies")
	public ReplyListResponse findReplies(@PathVariable Long parentId) {
		List<ReplyResult> results = commentFinder.findReplies(parentId);
		return ReplyListResponse.from(results);
	}

	@GetMapping("/cards/{cardId}/comments/likes")
	public CommentLikesResponse findLikeStates(@CurrentUserId Long userId, @PathVariable Long cardId) {
		List<CommentLikeResult> results = likeFinder.findLikeStates(userId, cardId);
		return CommentLikesResponse.from(results);
	}

	@PostMapping("/cards/{cardId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse createComment(@CurrentUserId Long userId, @PathVariable Long cardId,
	                                     @Valid @RequestBody CommentCreateRequest request) {
		CommentResult result = commentCreator.createComment(userId, cardId, request);
		return CommentResponse.from(result);
	}

	@PostMapping("/comments/{commentId}/replies")
	@ResponseStatus(HttpStatus.CREATED)
	public ReplyResponse createReply(@CurrentUserId Long userId, @PathVariable Long commentId,
	                                 @Valid @RequestBody CommentCreateRequest request) {
		ReplyResult result = commentCreator.createReply(userId, commentId, request);
		return ReplyResponse.from(result);
	}

	@PostMapping("/comments/{commentId}/like")
	public LikeStateResponse like(@CurrentUserId Long userId, @PathVariable Long commentId) {
		LikeStateResult result = likeUpdater.like(userId, commentId);
		return LikeStateResponse.from(result);
	}

	@DeleteMapping("/comments/{commentId}/like")
	public LikeStateResponse unlike(@CurrentUserId Long userId, @PathVariable Long commentId) {
		LikeStateResult result = likeUpdater.unlike(userId, commentId);
		return LikeStateResponse.from(result);
	}
}
