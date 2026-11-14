package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.domain.LikeSummary;
import java.util.List;
import java.util.Map;

public interface LikeFinder {

	/**
	 * ID 목록의 게시글 좋아요 수와 지정한 사용자의 좋아요 여부를 반환한다.
	 */
	Map<Long, LikeSummary> findPostLikes(Long userId, List<Long> postIds);

	/**
	 * ID 목록의 댓글·답글 좋아요 수와 지정한 사용자의 좋아요 여부를 반환한다.
	 */
	Map<Long, LikeSummary> findCommentLikes(Long userId, List<Long> commentIds);
}
