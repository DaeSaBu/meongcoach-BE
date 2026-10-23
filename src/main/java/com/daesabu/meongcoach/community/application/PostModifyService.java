package com.daesabu.meongcoach.community.application;

import static java.util.Optional.ofNullable;

import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.PostRegister;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostUpdateRequest;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.media.application.provided.StoredMediaVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostModifyService implements PostRegister {

	private final PostRepository postRepository;
	private final PostFinder postFinder;
	private final MentionTargetReaders mentionTargetReaders;
	private final StoredMediaVerifier storedMediaVerifier;

	@Override
	@Transactional
	public Post register(Long userId, PostRegisterRequest request) {
		Post post = Post.register(userId, request.toCommand());

		validateExistingMention(post);
		validateStoredMedia(userId, post);

		return postRepository.save(post);
	}

	@Override
	@Transactional
	public Post update(Long userId, Long postId, PostUpdateRequest request) {
		Post post = postFinder.findPost(userId, postId);
		post.update(request.toCommand());

		validateExistingMention(post);
		validateStoredMedia(userId, post);

		return post;
	}

	private void validateExistingMention(Post post) {
		ofNullable(post.getMention())
				.ifPresent(mentionTargetReaders::validateExists);
	}

	private void validateStoredMedia(Long userId, Post post) {
		post.getMedia()
				.forEach(media -> storedMediaVerifier.validateUrl(media.getType().getMediaScope(), userId, media.getUrl()));
	}
}
