package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.domain.Post;
import jakarta.validation.Valid;

/**
 * 게시글 조회 능력.
 */
public interface PostFinder {

	/**
	 * 게시글을 ID 내림차순으로 size개까지 반환한다. cursor가 있으면 그보다 작은 ID의 게시글만 반환한다.
	 * lessonId가 있으면 그 레슨을 멘션한 게시글만 반환한다.
	 * 다음 페이지가 있으면 nextCursor에 마지막 게시글 ID가 담기고, 없으면 null이다.
	 * 각 게시글에는 작성자 프로필 요약과 멘션 대상 요약이 함께 담기고, 게시글의 미디어는 등록 순서대로 조회된다.
	 * 프로필이 없는 작성자는 author가 null이고, 멘션이 없거나 멘션 대상이 없으면 mentionTarget이 null이다.
	 */
	PostListResult findPosts(@Valid PostListRequest request);

	/**
	 * 게시글 하나를 id로 조회한다. 없으면 PostNotFoundException을 던진다.
	 */
	Post findPost(Long postId);

	/**
	 * 사용자가 작성한 게시글 하나를 id로 조회한다. 없거나 삭제됐거나 다른 사용자의 게시글이면 PostNotFoundException을 던진다.
	 */
	Post findPost(Long userId, Long postId);

	/**
	 * 게시글 하나를 작성자 프로필 요약, 멘션 대상 요약과 함께 반환하며, 게시글의 미디어는 등록 순서대로 조회된다.
	 * 프로필이 없는 작성자는 author가 null이고, 멘션이 없거나 멘션 대상이 없으면 mentionTarget이 null이다.
	 * 게시글이 없으면 PostNotFoundException을 던진다.
	 */
	PostResult findPostDetail(Long postId);
}
