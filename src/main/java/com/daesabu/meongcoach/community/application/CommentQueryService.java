package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentResult;
import com.daesabu.meongcoach.community.application.required.CommentQueryRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
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

	private void validatePostExists(Long postId) {
		if (!postRepository.existsById(postId)) {
			throw new PostNotFoundException(postId);
		}
	}

	private List<CommentResult> compose(List<Comment> comments) {
		Map<Long, UserProfileSummaryResult> authors = findAuthors(comments);
		return comments.stream()
				.map(comment -> new CommentResult(comment, authors.get(comment.getUserId())))
				.toList();
	}

	private Map<Long, UserProfileSummaryResult> findAuthors(List<Comment> comments) {
		Set<Long> userIds = comments.stream()
				.map(Comment::getUserId)
				.collect(Collectors.toSet());
		return authorProfiles.findByUserIds(userIds);
	}

	private Long nextCursorOf(Slice<Comment> slice) {
		if (!slice.hasNext()) {
			return null;
		}
		Comment last = slice.getContent().getLast();
		return last.getId();
	}
}
