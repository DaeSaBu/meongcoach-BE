package com.daesabu.meongcoach.community.adapter.webapi;

import com.daesabu.meongcoach.community.adapter.webapi.dto.AuthorDogResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.BlockListResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.CommentListResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.CommentRegisterResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.CommentUpdateResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.LikeResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.MyAuthorResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.MyCommentListResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostListResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostMediaUploadUrlResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostRegisterResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.PostUpdateResponse;
import com.daesabu.meongcoach.community.adapter.webapi.dto.ReplyListResponse;
import com.daesabu.meongcoach.community.application.provided.AuthorFinder;
import com.daesabu.meongcoach.community.application.provided.BlockFinder;
import com.daesabu.meongcoach.community.application.provided.BlockRegister;
import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.CommentRegister;
import com.daesabu.meongcoach.community.application.provided.ContentReportRegister;
import com.daesabu.meongcoach.community.application.provided.LikeRegister;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.PostMediaUploadUrlIssuer;
import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorDogResult;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.community.application.provided.dto.BlockedUserResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentUpdateRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostMediaUploadUrlRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostUpdateRequest;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyListResult;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.LikeSummary;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
	private final LikeRegister likeRegister;
	private final PostRegister postRegister;
	private final CommentRegister commentRegister;
	private final CommentFinder commentFinder;
	private final PostMediaUploadUrlIssuer postMediaUploadUrlIssuer;
	private final AuthorFinder authorFinder;
	private final ContentReportRegister contentReportRegister;
	private final BlockRegister blockRegister;
	private final BlockFinder blockFinder;

	@GetMapping("/me")
	public MyAuthorResponse findMyAuthor(@CurrentUserId Long userId) {
		Optional<AuthorResult> author = authorFinder.findAuthor(userId);
		return MyAuthorResponse.from(author);
	}

	@GetMapping("/me/posts")
	public PostListResponse findMyPosts(@CurrentUserId Long userId, @Valid @ModelAttribute PostListRequest request) {
		PostListResult result = postFinder.findPostsByAuthor(userId, userId, request);
		return PostListResponse.from(result);
	}

	@GetMapping("/me/comments")
	public MyCommentListResponse findMyComments(@CurrentUserId Long userId,
	                                            @Valid @ModelAttribute CommentListRequest request) {
		CommentListResult result = commentFinder.findCommentsByAuthor(userId, userId, request);
		return MyCommentListResponse.from(result);
	}

	@GetMapping("/users/{userId}/dog")
	public AuthorDogResponse findAuthorDog(@PathVariable Long userId) {
		Optional<AuthorDogResult> dog = authorFinder.findAuthorDog(userId);
		return AuthorDogResponse.from(dog);
	}

	@GetMapping("/posts")
	public PostListResponse findPosts(@CurrentUserId Long userId, @Valid @ModelAttribute PostListRequest request) {
		PostListResult result = postFinder.findPosts(userId, request);
		return PostListResponse.from(result);
	}

	@GetMapping("/posts/{postId}")
	public PostResponse findPost(@CurrentUserId Long userId, @PathVariable Long postId) {
		PostResult result = postFinder.findPostDetail(userId, postId);
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

	@DeleteMapping("/posts/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePost(@CurrentUserId Long userId, @PathVariable Long postId) {
		postRegister.delete(userId, postId);
	}

	@PostMapping("/presigned-urls")
	public PostMediaUploadUrlResponse issueMediaUploadUrl(@CurrentUserId Long userId,
	                                                      @Valid @RequestBody PostMediaUploadUrlRequest request) {
		MediaUploadUrlResult result = postMediaUploadUrlIssuer.issue(userId, request);
		return PostMediaUploadUrlResponse.from(result);
	}

	@GetMapping("/posts/{postId}/comments")
	public CommentListResponse findComments(@CurrentUserId Long userId, @PathVariable Long postId,
	                                        @Valid @ModelAttribute CommentListRequest request) {
		CommentListResult result = commentFinder.findComments(userId, postId, request);
		return CommentListResponse.from(result);
	}

	@Deprecated(forRemoval = true)
	@GetMapping("/comments/{commentId}/replies")
	public ReplyListResponse findReplies(@CurrentUserId Long userId, @PathVariable Long commentId,
	                                     @Valid @ModelAttribute CommentListRequest request) {
		ReplyListResult result = commentFinder.findReplies(userId, commentId, request);
		return ReplyListResponse.from(result);
	}

	@PostMapping("/posts/{postId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentRegisterResponse registerComment(@CurrentUserId Long userId, @PathVariable Long postId,
	                                               @Valid @RequestBody CommentRegisterRequest request) {
		Comment comment = commentRegister.register(userId, postId, request);
		return CommentRegisterResponse.from(comment);
	}

	@PutMapping("/comments/{commentId}")
	public CommentUpdateResponse updateComment(@CurrentUserId Long userId, @PathVariable Long commentId,
	                                           @Valid @RequestBody CommentUpdateRequest request) {
		Comment comment = commentRegister.update(userId, commentId, request);
		return CommentUpdateResponse.from(comment);
	}

	@DeleteMapping("/comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteComment(@CurrentUserId Long userId, @PathVariable Long commentId) {
		commentRegister.delete(userId, commentId);
	}

	@PostMapping("/posts/{postId}/like")
	public LikeResponse likePost(@CurrentUserId Long userId, @PathVariable Long postId) {
		LikeSummary result = likeRegister.likePost(userId, postId);
		return LikeResponse.from(result);
	}

	@DeleteMapping("/posts/{postId}/like")
	public LikeResponse unlikePost(@CurrentUserId Long userId, @PathVariable Long postId) {
		LikeSummary result = likeRegister.unlikePost(userId, postId);
		return LikeResponse.from(result);
	}

	@PostMapping("/comments/{commentId}/like")
	public LikeResponse likeComment(@CurrentUserId Long userId, @PathVariable Long commentId) {
		LikeSummary result = likeRegister.likeComment(userId, commentId);
		return LikeResponse.from(result);
	}

	@DeleteMapping("/comments/{commentId}/like")
	public LikeResponse unlikeComment(@CurrentUserId Long userId, @PathVariable Long commentId) {
		LikeSummary result = likeRegister.unlikeComment(userId, commentId);
		return LikeResponse.from(result);
	}

	@PostMapping("/posts/{postId}/report")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reportPost(@CurrentUserId Long userId, @PathVariable Long postId,
	                       @Valid @RequestBody ReportRegisterRequest request) {
		contentReportRegister.reportPost(userId, postId, request);
	}

	@PostMapping("/comments/{commentId}/report")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reportComment(@CurrentUserId Long userId, @PathVariable Long commentId,
	                          @Valid @RequestBody ReportRegisterRequest request) {
		contentReportRegister.reportComment(userId, commentId, request);
	}

	@PostMapping("/users/{userId}/block")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void blockUser(@CurrentUserId Long userId, @PathVariable("userId") Long targetUserId) {
		blockRegister.block(userId, targetUserId);
	}

	@DeleteMapping("/users/{userId}/block")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unblockUser(@CurrentUserId Long userId, @PathVariable("userId") Long targetUserId) {
		blockRegister.unblock(userId, targetUserId);
	}

	@GetMapping("/blocks")
	public BlockListResponse findBlocks(@CurrentUserId Long userId) {
		List<BlockedUserResult> result = blockFinder.findBlockedUsers(userId);
		return BlockListResponse.from(result);
	}
}
