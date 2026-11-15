package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.LikeFinder;
import com.daesabu.meongcoach.community.application.provided.LikeRegister;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.required.CommentLikeRepository;
import com.daesabu.meongcoach.community.application.required.PostLikeRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentLike;
import com.daesabu.meongcoach.community.domain.LikeRegisteredEvent;
import com.daesabu.meongcoach.community.domain.LikeSummary;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostLike;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeModifyService implements LikeRegister {
	private final ApplicationEventPublisher eventPublisher;
	private final PostFinder postFinder;
	private final CommentFinder commentFinder;
	private final LikeFinder likeFinder;
	private final PostLikeRepository postLikeRepository;
	private final CommentLikeRepository commentLikeRepository;

	@Override
	@Transactional
	public LikeSummary likePost(Long userId, Long postId) {
		Post post = postFinder.findPostForUpdate(postId);
		registerOrRelikePost(userId, post);
		Map<Long, LikeSummary> likes = likeFinder.findPostLikes(userId, List.of(postId));
		return likes.get(postId);
	}

	@Override
	@Transactional
	public LikeSummary unlikePost(Long userId, Long postId) {
		postFinder.findPostForUpdate(postId);
		PostLike like = postLikeRepository.findByPostIdAndUserId(postId, userId).orElse(null);
		if (like != null) {
			like.unlike();
		}
		Map<Long, LikeSummary> likes = likeFinder.findPostLikes(userId, List.of(postId));
		return likes.get(postId);
	}

	@Override
	@Transactional
	public LikeSummary likeComment(Long userId, Long commentId) {
		Comment comment = commentFinder.findCommentForUpdate(commentId);
		registerOrRelikeComment(userId, comment);
		Map<Long, LikeSummary> likes = likeFinder.findCommentLikes(userId, List.of(commentId));
		return likes.get(commentId);
	}

	@Override
	@Transactional
	public LikeSummary unlikeComment(Long userId, Long commentId) {
		commentFinder.findCommentForUpdate(commentId);
		CommentLike like = commentLikeRepository.findByCommentIdAndUserId(commentId, userId).orElse(null);
		if (like != null) {
			like.unlike();
		}
		Map<Long, LikeSummary> likes = likeFinder.findCommentLikes(userId, List.of(commentId));
		return likes.get(commentId);
	}

	private void registerOrRelikePost(Long userId, Post post) {
		Long postId = post.getId();
		PostLike like = postLikeRepository.findByPostIdAndUserId(postId, userId).orElse(null);
		if (like != null) {
			like.relike();
			return;
		}

		PostLike newLike = PostLike.register(postId, userId);
		postLikeRepository.save(newLike);
		LikeRegisteredEvent event = new LikeRegisteredEvent(postId, post.getUserId(), userId, "게시글");
		eventPublisher.publishEvent(event);
	}

	private void registerOrRelikeComment(Long userId, Comment comment) {
		Long commentId = comment.getId();
		CommentLike like = commentLikeRepository.findByCommentIdAndUserId(commentId, userId).orElse(null);
		if (like != null) {
			like.relike();
			return;
		}

		CommentLike newLike = CommentLike.register(commentId, userId);
		commentLikeRepository.save(newLike);
		LikeRegisteredEvent event = new LikeRegisteredEvent(comment.getPost().getId(), comment.getUserId(), userId, "댓글");
		eventPublisher.publishEvent(event);
	}
}
