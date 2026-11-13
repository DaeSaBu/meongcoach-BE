package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.AuthorFinder;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorDogResult;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorQueryService implements AuthorFinder {

	private final AuthorProfiles authorProfiles;
	private final UserFinder userFinder;
	private final DogProfileFinder dogProfileFinder;

	@Override
	public Optional<AuthorResult> findAuthor(Long userId) {
		AuthorResult author = authorProfiles.findByUserIds(Set.of(userId)).get(userId);
		return Optional.ofNullable(author);
	}

	@Override
	public Optional<AuthorDogResult> findAuthorDog(Long userId) {
		Set<Long> guardianUserIds = userFinder.findProfileSummaries(Set.of(userId)).guardianUserIds();
		if (!guardianUserIds.contains(userId)) {
			return Optional.empty();
		}
		return dogProfileFinder.findSelectedPublicDog(userId)
				.map(AuthorDogResult::from);
	}
}
