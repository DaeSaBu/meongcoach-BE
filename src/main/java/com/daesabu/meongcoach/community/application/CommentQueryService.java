package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentResult;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyListResult;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyResult;
import com.daesabu.meongcoach.community.application.required.CommentClosureRepository;
import com.daesabu.meongcoach.community.application.required.CommentQueryRepository;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentClosure;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService implements CommentFinder {

	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final CommentClosureRepository commentClosureRepository;
	private final CommentQueryRepository commentQueryRepository;
	private final AuthorProfiles authorProfiles;

	@Override
	public CommentListResult findComments(Long postId, CommentListRequest request) {
		validatePostExists(postId);
		Slice<Comment> slice = commentQueryRepository.findRootComments(postId, request.cursor(), request.size());

		List<CommentResult> comments = compose(slice.getContent());
		Long nextCursor = nextCursorOf(slice);
		return new CommentListResult(comments, nextCursor);
	}

	@Override
	public ReplyListResult findReplies(Long commentId, CommentListRequest request) {
		validateRootComment(commentId);
		Slice<Comment> slice = commentQueryRepository.findReplies(commentId, request.cursor(), request.size());

		List<ReplyResult> replies = composeReplies(slice.getContent());
		Long nextCursor = nextCursorOf(slice);
		return new ReplyListResult(replies, nextCursor);
	}

	@Override
	public Comment findComment(Long userId, Long commentId) {
		return commentRepository.findActiveByIdAndUserId(commentId, userId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));
	}

	private void validatePostExists(Long postId) {
		if (!postRepository.existsByIdAndDeletedAtIsNull(postId)) {
			throw new PostNotFoundException(postId);
		}
	}

	private void validateRootComment(Long commentId) {
		boolean exists = commentRepository.existsByIdAndDeletedAtIsNull(commentId);
		boolean isReply = commentClosureRepository.existsByDescendantIdAndDepth(commentId, CommentClosure.REPLY_DEPTH);
		if (!exists || isReply) {
			throw new CommentNotFoundException(commentId);
		}
	}

	private List<CommentResult> compose(List<Comment> comments) {
		Map<Long, AuthorResult> authors = findAuthors(comments);
		Map<Long, Long> replyCounts = countReplies(comments);
		return comments.stream()
				.map(comment -> new CommentResult(comment, authors.get(comment.getUserId()),
						replyCounts.getOrDefault(comment.getId(), 0L)))
				.toList();
	}

	private List<ReplyResult> composeReplies(List<Comment> replies) {
		Map<Long, AuthorResult> authors = findAuthors(replies);
		return replies.stream()
				.map(reply -> new ReplyResult(reply, authors.get(reply.getUserId())))
				.toList();
	}

	private Map<Long, AuthorResult> findAuthors(List<Comment> comments) {
		Set<Long> userIds = comments.stream()
				.map(Comment::getUserId)
				.collect(Collectors.toSet());
		return authorProfiles.findByUserIds(userIds);
	}

	private Map<Long, Long> countReplies(List<Comment> comments) {
		List<Long> rootIds = comments.stream()
				.map(Comment::getId)
				.toList();
		return commentQueryRepository.countReplies(rootIds);
	}

	private Long nextCursorOf(Slice<Comment> slice) {
		if (!slice.hasNext()) {
			return null;
		}
		Comment last = slice.getContent().getLast();
		return last.getId();
	}
}
