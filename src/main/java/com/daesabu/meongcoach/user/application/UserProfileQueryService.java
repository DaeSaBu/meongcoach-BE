package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileQueryService implements UserProfileFinder {

	private final UserProfileRepository userProfileRepository;

	@Override
	public List<UserProfileSummaryResult> findSummaries(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return List.of();
		}
		return userProfileRepository.findAllById(userIds).stream()
				.map(UserProfileSummaryResult::from)
				.toList();
	}
}
