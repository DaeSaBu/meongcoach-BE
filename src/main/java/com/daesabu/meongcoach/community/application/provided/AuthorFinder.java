package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorDogResult;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import java.util.Optional;

/**
 * 커뮤니티 작성자 조회 능력.
 */
public interface AuthorFinder {

	/**
	 * 회원의 커뮤니티 작성자 요약을 게시글·댓글의 작성자와 같은 기준으로 반환한다.
	 * 프로필이나 선택된 강아지가 없으면 빈 값을 반환한다.
	 */
	Optional<AuthorResult> findAuthor(Long userId);

	/**
	 * 회원이 선택한 강아지의 공개 정보를 반환한다.
	 * 훈련사(EXPERT) 계정이거나 프로필·선택된 강아지가 없으면 빈 값을 반환한다.
	 */
	Optional<AuthorDogResult> findAuthorDog(Long userId);
}
