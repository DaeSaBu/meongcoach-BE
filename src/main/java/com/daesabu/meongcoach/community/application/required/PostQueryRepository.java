package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Post;
import org.springframework.data.domain.Slice;

/**
 * 조건이 선택적인 게시글 조회.
 */
public interface PostQueryRepository {

	/**
	 * 삭제되지 않은 게시글을 ID 내림차순으로 size개까지 조회한다. 조회하는 회원이 차단한 회원의 게시글은 조회하지 않는다. cursor가 있으면 그보다 작은 ID의 게시글만, lessonId가 있으면 그
	 * 레슨을 멘션한 게시글만 조회한다.
	 */
	Slice<Post> findPosts(Long viewerId, Long cursor, Long lessonId, int size);

	/**
	 * 사용자가 작성하고 삭제되지 않은 게시글을 ID 내림차순으로 size개까지 조회한다. cursor가 있으면 그보다 작은 ID의 게시글만, lessonId가 있으면 그 레슨을 멘션한 게시글만 조회한다.
	 */
	Slice<Post> findPostsByUserId(Long userId, Long cursor, Long lessonId, int size);
}
