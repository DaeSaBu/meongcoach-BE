package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import com.daesabu.meongcoach.training.application.provided.CardFinder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostRegisterService implements PostRegister {

	private final PostRepository postRepository;
	private final CardFinder cardFinder;

	@Override
	@Transactional
	public Post register(Long userId, PostRegisterRequest request) {
		Post post = Post.write(userId, request.toCommand());
		post.findMentionTargetId(MentionTargetType.CARD)
				.ifPresent(this::validateCardExists);
		return postRepository.save(post);
	}

	private void validateCardExists(Long cardId) {
		if (cardFinder.findSummaries(List.of(cardId)).isEmpty()) {
			throw new MentionTargetNotFoundException(cardId);
		}
	}
}
