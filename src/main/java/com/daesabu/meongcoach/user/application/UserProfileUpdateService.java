package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.media.application.provided.StoredImageUrlValidator;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileUpdateRequest;
import com.daesabu.meongcoach.user.application.provided.UserProfileUpdater;
import com.daesabu.meongcoach.user.domain.UserProfile;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileUpdateService implements UserProfileUpdater {

	private final UserProfileFinder userProfileFinder;
	private final StoredImageUrlValidator storedImageUrlValidator;

	@Override
	@Transactional
	public UserProfile update(Long userId, UserProfileUpdateRequest request) {
		storedImageUrlValidator.validate(request.profileImageUrl());
		UserProfile profile = userProfileFinder.findProfile(userId);
		profile.updateProfile(request.toCommand());
		return profile;
	}

	@Override
	@Transactional
	public void addPriorTrainingTopics(Long userId, Collection<Long> topicIds) {
		UserProfile profile = userProfileFinder.findProfile(userId);
		profile.addPriorTrainingTopics(topicIds);
	}
}
