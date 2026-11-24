package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.BlockFinder;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.community.application.provided.dto.BlockedUserResult;
import com.daesabu.meongcoach.community.application.required.UserBlockRepository;
import com.daesabu.meongcoach.community.domain.UserBlock;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockQueryService implements BlockFinder {

	private final UserBlockRepository userBlockRepository;
	private final AuthorProfiles authorProfiles;

	@Override
	public List<BlockedUserResult> findBlockedUsers(Long userId) {
		List<Long> blockedIds = userBlockRepository.findByBlockerIdOrderByIdDesc(userId).stream()
				.map(UserBlock::getBlockedId)
				.toList();
		Map<Long, AuthorResult> authors = authorProfiles.findByUserIds(blockedIds);
		return blockedIds.stream()
				.map(blockedId -> new BlockedUserResult(blockedId, authors.get(blockedId)))
				.toList();
	}

	@Override
	public boolean isBlocked(Long userId, Long targetUserId) {
		return userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetUserId);
	}
}
