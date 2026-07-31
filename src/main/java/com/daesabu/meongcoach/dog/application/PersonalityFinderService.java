package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.PersonalityFinder;
import com.daesabu.meongcoach.dog.application.provided.PersonalityInfo;
import com.daesabu.meongcoach.dog.domain.Personality;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 강아지 성격 목록을 조회한다.
 */
@Service
public class PersonalityFinderService implements PersonalityFinder {

	@Override
	public List<PersonalityInfo> findAll() {
		return Arrays.stream(Personality.values())
				.map(PersonalityInfo::from)
				.toList();
	}
}
