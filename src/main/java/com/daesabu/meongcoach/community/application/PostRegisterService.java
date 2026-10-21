package com.daesabu.meongcoach.community.application;

import static java.util.Optional.ofNullable;

import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Post;
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
	private final MentionTargetReaders mentionTargetReaders;

	@Override
	@Transactional
	public Post register(Long userId, PostRegisterRequest request) {
		Post post = Post.register(userId, request.toCommand());

		validateExistingMention(post);

		return postRepository.save(post);
	}

	private void validateExistingMention(Post post) {
		ofNullable(post.getMention())
				.ifPresent(mentionTargetReaders::validateExists);
	}
}
