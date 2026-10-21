package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.CommentClosure;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentClosureRepository extends JpaRepository<CommentClosure, Long> {

	/**
	 * 댓글을 자손으로 갖는 경로 중 depth가 가장 큰 경로를 조회한다. 경로의 ancestor가 그 댓글의 루트 댓글이다.
	 */
	Optional<CommentClosure> findFirstByDescendantIdOrderByDepthDesc(Long descendantId);
}
