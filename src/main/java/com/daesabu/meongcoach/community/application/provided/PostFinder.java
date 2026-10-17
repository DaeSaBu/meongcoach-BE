package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import jakarta.validation.Valid;

/**
 * 게시글 조회 능력.
 */
public interface PostFinder {

	/**
	 * 게시글을 ID 내림차순으로 size개까지 반환한다. cursor가 있으면 그보다 작은 ID의 게시글만 반환한다.
	 * 다음 페이지가 있으면 hasNext가 true이고 nextCursor에 마지막 게시글 ID가 담기며, 없으면 nextCursor는 null이다.
	 * 각 게시글에는 작성자 프로필 요약과 멘션한 카드 요약이 함께 담긴다.
	 * 프로필이 없는 작성자는 author가 null이고, 멘션이 없거나 멘션한 카드가 없으면 mentionedCard가 null이다.
	 */
	PostListResult findPosts(@Valid PostListRequest request);
}
