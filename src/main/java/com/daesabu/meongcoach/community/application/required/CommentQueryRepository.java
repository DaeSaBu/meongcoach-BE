package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Comment;
import org.springframework.data.domain.Slice;

/**
 * 댓글 트리 구조를 따라가는 댓글 조회.
 */
public interface CommentQueryRepository {

	/**
	 * 게시글의 삭제되지 않은 루트 댓글을 ID 오름차순으로 size개까지 조회한다. cursor가 있으면 그보다 큰 ID의 댓글만 조회한다.
	 */
	Slice<Comment> findRootComments(Long postId, Long cursor, int size);
}
