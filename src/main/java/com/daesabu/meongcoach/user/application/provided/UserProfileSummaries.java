package com.daesabu.meongcoach.user.application.provided;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record UserProfileSummaries(List<UserProfileSummaryResult> profiles) {

	public List<UserProfileSummaryResult> experts() {
		return profiles.stream()
				.filter(UserProfileSummaryResult::expert)
				.toList();
	}

	public Set<Long> nonExpertUserIds() {
		return profiles.stream()
				.filter(profile -> !profile.expert())
				.map(UserProfileSummaryResult::userId)
				.collect(Collectors.toSet());
	}
}
