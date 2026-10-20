package com.daesabu.meongcoach.community.application;

import static java.util.Optional.ofNullable;

import com.daesabu.meongcoach.community.application.provided.CommentRegister;
import com.daesabu.meongcoach.community.application.provided.dto.CommentRegisterRequest;
import com.daesabu.meongcoach.community.application.required.CommentClosureRepository;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentClosure;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentRegisterService implements CommentRegister {

	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final CommentClosureRepository commentClosureRepository;

	@Override
	@Transactional
	public Comment register(Long userId, Long postId, CommentRegisterRequest request) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new PostNotFoundException(postId));
		Comment comment = Comment.write(post, userId, request.content());
		Optional<Comment> root = findRoot(postId, request.parentId());

		commentRepository.save(comment);
		commentClosureRepository.save(CommentClosure.self(comment));
		root.ifPresent(it -> commentClosureRepository.save(CommentClosure.reply(it, comment)));

		return comment;
	}

	private Optional<Comment> findRoot(Long postId, Long parentId) {
		return ofNullable(parentId)
				.map(id -> findParent(postId, id))
				.map(this::findRootOf);
	}

	private Comment findParent(Long postId, Long parentId) {
		return commentRepository.findByIdAndPostIdAndDeletedAtIsNull(parentId, postId)
				.orElseThrow(() -> new CommentNotFoundException(parentId));
	}

	private Comment findRootOf(Comment parent) {
		return commentClosureRepository.findFirstByDescendantIdOrderByDepthDesc(parent.getId())
				.map(CommentClosure::getAncestor)
				.orElseThrow();
	}
}
