package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.CommentCountResult;
import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.CommentPageResult;
import com.daesabu.meongcoach.community.application.provided.CommentResult;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.application.required.CommentSummary;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentTarget;
import com.daesabu.meongcoach.community.domain.CommentTargetType;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 조회 서비스. 집계가 끝난 한 페이지를 리포지토리에서 받아 작성자 닉네임만 user 모듈에서 합친다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService implements CommentFinder {

	// 페이지 크기는 클라이언트가 정하지 않는다
	private static final Pageable PAGE = PageRequest.ofSize(20);

	private final CommentRepository commentRepository;

	private final CommentTargetChecker targetChecker;

	private final UserProfileFinder userProfileFinder;

	@Override
	public CommentPageResult findComments(Long userId, CommentTarget target, Long cursor) {
		targetChecker.verify(target);
		Slice<CommentSummary> slice = commentRepository.findTopLevel(target, userId, cursor, PAGE);
		long totalCount = commentRepository.countByTarget(target);
		return toPage(slice, totalCount);
	}

	@Override
	public CommentPageResult findReplies(Long userId, Long commentId, Long cursor) {
		Comment root = commentRepository.getComment(commentId);
		if (root.isReply()) {
			throw new CommentNotFoundException(commentId);
		}
		Slice<CommentSummary> slice = commentRepository.findReplies(commentId, userId, cursor, PAGE);
		long totalCount = commentRepository.countByRootId(commentId);
		return toPage(slice, totalCount);
	}

	@Override
	public List<CommentCountResult> countComments(CommentTargetType type, Set<Long> targetIds) {
		return commentRepository.countByTargetIds(type, targetIds);
	}

	private CommentPageResult toPage(Slice<CommentSummary> slice, long totalCount) {
		List<CommentResult> comments = toResults(slice.getContent());
		Long nextCursor = nextCursor(slice);
		return new CommentPageResult(comments, totalCount, nextCursor);
	}

	// 다음 페이지가 있으면 마지막으로 내려준 항목의 id가 다음 커서다
	private Long nextCursor(Slice<CommentSummary> slice) {
		if (!slice.hasNext()) {
			return null;
		}
		return slice.getContent().getLast().id();
	}

	private List<CommentResult> toResults(List<CommentSummary> rows) {
		Map<Long, String> nicknames = findNicknames(rows);
		return rows.stream()
				.map(row -> toResult(row, nicknames.get(row.authorId())))
				.toList();
	}

	private Map<Long, String> findNicknames(List<CommentSummary> rows) {
		Set<Long> authorIds = rows.stream()
				.map(CommentSummary::authorId)
				.collect(Collectors.toSet());
		return userProfileFinder.findNicknames(authorIds);
	}

	private CommentResult toResult(CommentSummary row, String authorNickname) {
		return new CommentResult(row.id(), row.parentId(), row.authorId(), authorNickname, row.content(),
				row.createdAt(), row.replyCount(), row.likeCount(), row.likedByMe());
	}
}
