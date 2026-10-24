package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorProfiles {

	private final UserFinder userFinder;
	private final DogProfileFinder dogProfileFinder;

	public Map<Long, AuthorResult> findByUserIds(Collection<Long> userIds) {
		Set<Long> profiledUserIds = userFinder.findProfileSummaries(userIds).stream()
				.map(UserProfileSummaryResult::userId)
				.collect(Collectors.toSet());
		return dogProfileFinder.findFirstDogs(profiledUserIds).stream()
				.map(AuthorResult::from)
				.collect(Collectors.toMap(AuthorResult::userId, Function.identity()));
	}
}
