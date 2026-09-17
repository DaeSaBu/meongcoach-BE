package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.PersonalityFinder;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 강아지 성격 목록을 조회한다.
 * 표시 순서는 enum 선언 순서를 그대로 따른다.
 */
@Service
public class PersonalityFinderService implements PersonalityFinder {

	@Override
	public List<Personality> findAll() {
		return List.of(Personality.values());
	}
}
