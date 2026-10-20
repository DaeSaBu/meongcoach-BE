package com.daesabu.meongcoach.community.application;

import static java.util.Optional.ofNullable;

import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.application.required.PostQueryRepository;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostMention;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService implements PostFinder {

	private final PostQueryRepository postQueryRepository;
	private final UserProfileFinder userProfileFinder;
	private final MentionTargetReaders mentionTargetReaders;

	@Override
	public PostListResult findPosts(PostListRequest request) {
		Slice<Post> slice = getSlice(request);

		List<PostResult> posts = compose(slice.getContent());
		Long nextCursor = nextCursorOf(slice);
		return new PostListResult(posts, nextCursor);
	}

	private Slice<Post> getSlice(PostListRequest request) {
		return postQueryRepository.findPosts(request.cursor(), request.size());
	}

	private Long nextCursorOf(Slice<Post> slice) {
		if (!slice.hasNext()) {
			return null;
		}
		Post last = slice.getContent().getLast();
		return last.getId();
	}

	private List<PostResult> compose(List<Post> posts) {
		Map<Long, UserProfileSummaryResult> authors = findAuthors(posts);
		Map<MentionTargetType, Map<Long, MentionTargetResult>> mentionTargets = findMentionTargets(posts);
		return posts.stream()
				.map(post -> toResult(post, authors, mentionTargets))
				.toList();
	}

	private Map<Long, UserProfileSummaryResult> findAuthors(List<Post> posts) {
		Set<Long> userIds = posts.stream()
				.map(Post::getUserId)
				.collect(Collectors.toSet());
		return userProfileFinder.findSummaries(userIds).stream()
				.collect(Collectors.toMap(UserProfileSummaryResult::userId, Function.identity()));
	}

	private Map<MentionTargetType, Map<Long, MentionTargetResult>> findMentionTargets(List<Post> posts) {
		List<PostMention> mentions = posts.stream()
				.map(Post::getMention)
				.filter(Objects::nonNull)
				.toList();
		return mentionTargetReaders.findSummaries(mentions);
	}

	private PostResult toResult(Post post, Map<Long, UserProfileSummaryResult> authors,
	                            Map<MentionTargetType, Map<Long, MentionTargetResult>> mentionTargets) {
		UserProfileSummaryResult author = authors.get(post.getUserId());
		MentionTargetResult mentionTarget = ofNullable(post.getMention())
				.map(mention -> mentionTargets.get(mention.getTargetType()).get(mention.getTargetId()))
				.orElse(null);
		return new PostResult(post, author, mentionTarget);
	}
}
