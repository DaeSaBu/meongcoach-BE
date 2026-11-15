package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.LikeFinder;
import com.daesabu.meongcoach.community.application.provided.LikeRegister;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.required.CommentLikeRepository;
import com.daesabu.meongcoach.community.application.required.PostLikeRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.LikeRegisteredEvent;
import com.daesabu.meongcoach.community.domain.LikeStatus;
import com.daesabu.meongcoach.community.domain.LikeSummary;
import com.daesabu.meongcoach.community.domain.Post;
import java.time.LocalDateTime;
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
		Post post = postFinder.findPost(postId);
		int inserted = postLikeRepository.insertIfAbsent(postId, userId, LocalDateTime.now());
		postLikeRepository.updateStatus(postId, userId, LikeStatus.LIKED, LocalDateTime.now());
		if (inserted > 0) {
			eventPublisher.publishEvent(new LikeRegisteredEvent(postId, post.getUserId(), userId, "게시글"));
		}
		Map<Long, LikeSummary> likes = likeFinder.findPostLikes(userId, List.of(postId));
		return likes.get(postId);
	}

	@Override
	@Transactional
	public LikeSummary unlikePost(Long userId, Long postId) {
		postFinder.findPost(postId);
		postLikeRepository.updateStatus(postId, userId, LikeStatus.UNLIKED, LocalDateTime.now());
		Map<Long, LikeSummary> likes = likeFinder.findPostLikes(userId, List.of(postId));
		return likes.get(postId);
	}

	@Override
	@Transactional
	public LikeSummary likeComment(Long userId, Long commentId) {
		Comment comment = commentFinder.findComment(commentId);
		int inserted = commentLikeRepository.insertIfAbsent(commentId, userId, LocalDateTime.now());
		commentLikeRepository.updateStatus(commentId, userId, LikeStatus.LIKED, LocalDateTime.now());
		if (inserted > 0) {
			eventPublisher.publishEvent(new LikeRegisteredEvent(comment.getPost().getId(), comment.getUserId(), userId, "댓글"));
		}
		Map<Long, LikeSummary> likes = likeFinder.findCommentLikes(userId, List.of(commentId));
		return likes.get(commentId);
	}

	@Override
	@Transactional
	public LikeSummary unlikeComment(Long userId, Long commentId) {
		commentFinder.findComment(commentId);
		commentLikeRepository.updateStatus(commentId, userId, LikeStatus.UNLIKED, LocalDateTime.now());
		Map<Long, LikeSummary> likes = likeFinder.findCommentLikes(userId, List.of(commentId));
		return likes.get(commentId);
	}
}
