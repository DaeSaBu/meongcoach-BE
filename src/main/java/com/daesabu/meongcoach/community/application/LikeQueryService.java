package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.LikeFinder;
import com.daesabu.meongcoach.community.application.required.CommentLikeRepository;
import com.daesabu.meongcoach.community.application.required.LikeCount;
import com.daesabu.meongcoach.community.application.required.PostLikeRepository;
import com.daesabu.meongcoach.community.domain.LikeSummary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeQueryService implements LikeFinder {
	private final PostLikeRepository postLikeRepository;
	private final CommentLikeRepository commentLikeRepository;

	@Override
	public Map<Long, LikeSummary> findPostLikes(Long userId, List<Long> postIds) {
		if (postIds.isEmpty()) {
			return Map.of();
		}
		List<LikeCount> counts = postLikeRepository.countByTargetIds(postIds);
		List<Long> likedIds = postLikeRepository.findLikedTargetIds(userId, postIds);
		return compose(postIds, counts, likedIds);
	}

	@Override
	public Map<Long, LikeSummary> findCommentLikes(Long userId, List<Long> commentIds) {
		if (commentIds.isEmpty()) {
			return Map.of();
		}
		List<LikeCount> counts = commentLikeRepository.countByTargetIds(commentIds);
		List<Long> likedIds = commentLikeRepository.findLikedTargetIds(userId, commentIds);
		return compose(commentIds, counts, likedIds);
	}

	private Map<Long, LikeSummary> compose(List<Long> ids, List<LikeCount> counts, List<Long> likedIds) {
		Map<Long, Long> countById = new HashMap<>();
		counts.forEach(count -> countById.put(count.getTargetId(), count.getLikeCount()));
		Set<Long> liked = new HashSet<>(likedIds);
		Map<Long, LikeSummary> result = new HashMap<>();
		for (Long id : ids) {
			result.put(id, new LikeSummary(countById.getOrDefault(id, 0L), liked.contains(id)));
		}
		return result;
	}
}
