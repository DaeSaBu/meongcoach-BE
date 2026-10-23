package com.daesabu.meongcoach.community.application.required;

import com.daesabu.meongcoach.community.domain.Post;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

	/**
	 * 삭제되지 않은 게시글을 조회한다. 없거나 삭제됐으면 빈 Optional을 반환한다.
	 */
	Optional<Post> findByIdAndDeletedAtIsNull(Long id);

	/**
	 * 삭제되지 않은 게시글이 있는지 확인한다.
	 */
	boolean existsByIdAndDeletedAtIsNull(Long id);

	/**
	 * 사용자가 작성하고 삭제되지 않은 게시글을 조회한다. 없거나 삭제됐거나 다른 사용자의 게시글이면 빈 Optional을 반환한다.
	 */
	Optional<Post> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
