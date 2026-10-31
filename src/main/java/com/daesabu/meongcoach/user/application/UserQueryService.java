package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryService implements UserFinder {
	private final UserRepository userRepository;

	@Override
	public boolean isActiveUser(Long userId) {
		User user = findById(userId);
		return user.isActive();
	}

	@Override
	public User findById(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
	}

	@Override
	public List<UserProfileSummaryResult> findProfileSummaries(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return List.of();
		}
		return userRepository.findAllById(userIds).stream()
				.filter(user -> user.getProfile() != null)
				.map(UserProfileSummaryResult::from)
				.toList();
	}

	@Override
	public Set<Long> findActiveUserIds(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Set.of();
		}
		return userRepository.findIdsByIdInAndStatus(userIds, UserStatus.ACTIVE);
	}
}
