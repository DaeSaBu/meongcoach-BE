package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.training.application.provided.dto.CardSummaryResult;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService implements PostFinder {

	private final PostRepository postRepository;
	private final UserProfileFinder userProfileFinder;
	private final CardFinder cardFinder;

	@Override
	public PostListResult findPosts(PostListRequest request) {
		Slice<Post> slice = getSlice(request);

		List<PostResult> posts = compose(slice.getContent());
		Long nextCursor = nextCursorOf(slice);
		return new PostListResult(posts, nextCursor);
	}

	private Slice<Post> getSlice(PostListRequest request) {
		Pageable pageable = PageRequest.of(0, request.size());

		if (request.cursor() == null) {
			return postRepository.findAllByOrderByIdDesc(pageable);
		}
		return postRepository.findAllByIdLessThanOrderByIdDesc(request.cursor(), pageable);
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
		Map<Long, CardSummaryResult> cards = findMentionedCards(posts);
		return posts.stream()
				.map(post -> toResult(post, authors, cards))
				.toList();
	}

	private Map<Long, UserProfileSummaryResult> findAuthors(List<Post> posts) {
		Set<Long> userIds = posts.stream()
				.map(Post::getUserId)
				.collect(Collectors.toSet());
		return userProfileFinder.findSummaries(userIds).stream()
				.collect(Collectors.toMap(UserProfileSummaryResult::userId, Function.identity()));
	}

	private Map<Long, CardSummaryResult> findMentionedCards(List<Post> posts) {
		Set<Long> cardIds = posts.stream()
				.map(post -> post.findMentionTargetId(MentionTargetType.CARD))
				.flatMap(Optional::stream)
				.collect(Collectors.toSet());
		return cardFinder.findSummaries(cardIds).stream()
				.collect(Collectors.toMap(CardSummaryResult::id, Function.identity()));
	}

	private PostResult toResult(Post post, Map<Long, UserProfileSummaryResult> authors,
	                            Map<Long, CardSummaryResult> cards) {
		UserProfileSummaryResult author = authors.get(post.getUserId());
		CardSummaryResult mentionedCard = post.findMentionTargetId(MentionTargetType.CARD)
				.map(cards::get)
				.orElse(null);
		return new PostResult(post, author, mentionedCard);
	}
}
