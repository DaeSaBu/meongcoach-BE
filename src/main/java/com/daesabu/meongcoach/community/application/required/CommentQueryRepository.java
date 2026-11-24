package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Comment;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Slice;

/**
 * 댓글 트리 구조를 따라가는 댓글 조회.
 */
public interface CommentQueryRepository {

	/**
	 * 게시글의 삭제되지 않은 루트 댓글을 ID 오름차순으로 size개까지 조회한다. 조회하는 회원이 차단한 회원의 댓글은 조회하지 않는다. cursor가 있으면 그보다 큰 ID의 댓글만 조회한다.
	 */
	Slice<Comment> findRootComments(Long viewerId, Long postId, Long cursor, int size);

	/**
	 * 루트 댓글 ID별 삭제되지 않은 답글 수를 반환한다. 조회하는 회원이 차단한 회원의 답글은 세지 않는다. 답글이 없는 루트 댓글은 결과에 없다.
	 */
	Map<Long, Long> countReplies(Long viewerId, Collection<Long> rootIds);

	/**
	 * 루트 댓글의 삭제되지 않은 답글을 ID 오름차순으로 size개까지 조회한다. 조회하는 회원이 차단한 회원의 답글은 조회하지 않는다. cursor가 있으면 그보다 큰 ID의 답글만 조회한다.
	 */
	Slice<Comment> findReplies(Long viewerId, Long rootId, Long cursor, int size);

	/**
	 * 루트 댓글의 삭제되지 않은 답글을 모두 조회한다. 답글이 없거나 루트 댓글이 아니면 빈 목록을 반환한다.
	 */
	List<Comment> findActiveReplies(Long rootId);

	/**
	 * 사용자가 작성하고 삭제되지 않은 댓글과 답글을 ID 내림차순으로 size개까지 조회한다. cursor가 있으면 그보다 작은 ID의 댓글만 조회한다. 삭제된 게시글의 댓글과 조회하는 회원이 차단한 회원의
	 * 게시글에 단 댓글은 조회하지 않는다.
	 */
	Slice<Comment> findCommentsByUserId(Long viewerId, Long userId, Long cursor, int size);
}
