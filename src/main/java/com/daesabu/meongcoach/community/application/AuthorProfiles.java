package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorProfiles {

	private final UserFinder userFinder;

	public Map<Long, UserProfileSummaryResult> findByUserIds(Collection<Long> userIds) {
		return userFinder.findProfileSummaries(userIds).stream()
				.collect(Collectors.toMap(UserProfileSummaryResult::userId, Function.identity()));
	}
}
