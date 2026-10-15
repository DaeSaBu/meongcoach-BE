package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.exception.UserProfileNotFoundException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileQueryService implements UserProfileFinder {

	private final UserProfileRepository userProfileRepository;

	@Override
	public UserProfile findProfile(Long userId) {
		return userProfileRepository.findById(userId)
				.orElseThrow(() -> new UserProfileNotFoundException(userId));
	}

	@Override
	public Set<Long> findPriorTrainingTopicIds(Long userId) {
		UserProfile profile = findProfile(userId);
		return Set.copyOf(profile.getPriorTrainingTopicIds());
	}
}
