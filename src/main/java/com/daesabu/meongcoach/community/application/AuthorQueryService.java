package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.AuthorFinder;
import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
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

	@Override
	public Optional<AuthorResult> findAuthor(Long userId) {
		AuthorResult author = authorProfiles.findByUserIds(Set.of(userId)).get(userId);
		return Optional.ofNullable(author);
	}
}
