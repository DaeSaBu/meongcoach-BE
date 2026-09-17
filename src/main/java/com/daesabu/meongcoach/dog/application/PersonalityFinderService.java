package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.PersonalityFinder;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 강아지 성격 목록을 조회한다.
 * 표시 순서는 enum 선언 순서를 그대로 따른다.
 * 성격은 enum 상수라 런타임에 늘지 않으므로 클래스 로딩 시 한 번만 목록을 만들어 두고 매 요청에는 그대로 돌려준다.
 */
@Service
public class PersonalityFinderService implements PersonalityFinder {

	private static final List<Personality> ALL = List.of(Personality.values());

	@Override
	public List<Personality> findAll() {
		return ALL;
	}
}
