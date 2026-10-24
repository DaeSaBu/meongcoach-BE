package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.community.application.provided.dto.CommentRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentUpdateRequest;
import com.daesabu.meongcoach.community.domain.Comment;
import jakarta.validation.Valid;

/**
 * 댓글 등록·수정·삭제 능력.
 */
public interface CommentRegister {

	/**
	 * 사용자의 댓글을 게시글에 작성하고 저장한 댓글을 반환한다. parentId가 있으면 그 댓글의 루트 댓글에 대한 답글로 작성한다.
	 * 게시글이 없거나 삭제됐으면 PostNotFoundException, parentId의 댓글이 없거나 삭제됐거나 다른 게시글의 댓글이면
	 * CommentNotFoundException, 본문이 비어 있거나 500자를 넘으면 InvalidCommentContentException을 던진다.
	 */
	Comment register(Long userId, Long postId, @Valid CommentRegisterRequest request);

	/**
	 * 사용자가 작성한 댓글의 본문을 요청 값으로 교체하고 수정된 댓글을 반환한다.
	 * 댓글이 없거나 삭제됐거나 본인이 작성한 댓글이 아니거나 게시글이 삭제됐으면 CommentNotFoundException,
	 * 본문이 비어 있거나 500자를 넘으면 InvalidCommentContentException을 던진다.
	 */
	Comment update(Long userId, Long commentId, @Valid CommentUpdateRequest request);

	/**
	 * 사용자가 작성한 댓글을 삭제한다. 댓글이 없거나 이미 삭제됐거나 본인이 작성한 댓글이 아니거나
	 * 게시글이 삭제됐으면 CommentNotFoundException을 던진다. 루트 댓글을 삭제해도 답글은 삭제하지 않는다.
	 */
	void delete(Long userId, Long commentId);
}
