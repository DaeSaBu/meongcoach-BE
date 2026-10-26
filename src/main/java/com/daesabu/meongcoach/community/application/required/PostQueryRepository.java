package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Post;
import org.springframework.data.domain.Slice;

/**
 * 조건이 선택적인 게시글 조회.
 */
public interface PostQueryRepository {

	/**
	 * 삭제되지 않은 게시글을 ID 내림차순으로 size개까지 조회한다. cursor가 있으면 그보다 작은 ID의 게시글만,
	 * lessonId가 있으면 그 레슨을 멘션한 게시글만 조회한다.
	 */
	Slice<Post> findPosts(Long cursor, Long lessonId, int size);
}
