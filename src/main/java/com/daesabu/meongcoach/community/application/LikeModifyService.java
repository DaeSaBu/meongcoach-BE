package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.LikeFinder;
import com.daesabu.meongcoach.community.application.provided.LikeRegister;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.required.CommentLikeRepository;
import com.daesabu.meongcoach.community.application.required.PostLikeRepository;
import com.daesabu.meongcoach.community.domain.LikeSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeModifyService implements LikeRegister {
	private final PostFinder postFinder;
	private final CommentFinder commentFinder;
	private final LikeFinder likeFinder;
	private final PostLikeRepository postLikeRepository;
	private final CommentLikeRepository commentLikeRepository;

	@Override
	@Transactional
	public LikeSummary likePost(Long userId, Long postId) {
		postFinder.findPost(postId);
		postLikeRepository.insertIfAbsent(postId, userId, LocalDateTime.now());
		Map<Long, LikeSummary> likes = likeFinder.findPostLikes(userId, List.of(postId));
		return likes.get(postId);
	}

	@Override
	@Transactional
	public LikeSummary unlikePost(Long userId, Long postId) {
		postFinder.findPost(postId);
		postLikeRepository.deleteLike(postId, userId);
		Map<Long, LikeSummary> likes = likeFinder.findPostLikes(userId, List.of(postId));
		return likes.get(postId);
	}

	@Override
	@Transactional
	public LikeSummary likeComment(Long userId, Long commentId) {
		commentFinder.findComment(commentId);
		commentLikeRepository.insertIfAbsent(commentId, userId, LocalDateTime.now());
		Map<Long, LikeSummary> likes = likeFinder.findCommentLikes(userId, List.of(commentId));
		return likes.get(commentId);
	}

	@Override
	@Transactional
	public LikeSummary unlikeComment(Long userId, Long commentId) {
		commentFinder.findComment(commentId);
		commentLikeRepository.deleteLike(commentId, userId);
		Map<Long, LikeSummary> likes = likeFinder.findCommentLikes(userId, List.of(commentId));
		return likes.get(commentId);
	}
}
