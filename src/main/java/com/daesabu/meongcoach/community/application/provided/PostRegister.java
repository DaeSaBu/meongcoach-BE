package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.domain.Post;
import jakarta.validation.Valid;

/**
 * 게시글 등록 능력.
 */
public interface PostRegister {

	/**
	 * 사용자의 게시글을 작성하고 저장한 게시글을 반환한다. mention이 없으면 멘션 없이 작성한다.
	 * 본문이 비어 있거나 500자를 넘으면 InvalidPostContentException, 멘션 대상 종류가 지원되지 않으면
	 * InvalidMentionTargetTypeException, 멘션한 레슨이 없으면 MentionTargetNotFoundException을 던진다.
	 */
	Post register(Long userId, @Valid PostRegisterRequest request);
}
