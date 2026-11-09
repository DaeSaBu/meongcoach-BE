package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaries;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorProfiles {

	private final UserFinder userFinder;
	private final DogProfileFinder dogProfileFinder;

	public Map<Long, AuthorResult> findByUserIds(Collection<Long> userIds) {
		UserProfileSummaries profiles = userFinder.findProfileSummaries(userIds);
		Stream<AuthorResult> experts = profiles.experts().stream()
				.map(AuthorResult::fromExpert);
		Stream<AuthorResult> guardians = dogProfileFinder.findSelectedDogs(profiles.guardianUserIds()).stream()
				.map(AuthorResult::from);
		return Stream.concat(experts, guardians)
				.collect(Collectors.toMap(AuthorResult::userId, Function.identity()));
	}
}
