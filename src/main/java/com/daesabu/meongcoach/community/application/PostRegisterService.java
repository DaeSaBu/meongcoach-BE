package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Post;
import java.util.Optional;
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
	private final MentionTargetVerifiers mentionTargetVerifiers;

	@Override
	@Transactional
	public Post register(Long userId, PostRegisterRequest request) {
		Post post = Post.write(userId, request.toCommand());
		Optional.ofNullable(post.getMention())
				.ifPresent(mentionTargetVerifiers::validateExists);
		return postRepository.save(post);
	}
}
