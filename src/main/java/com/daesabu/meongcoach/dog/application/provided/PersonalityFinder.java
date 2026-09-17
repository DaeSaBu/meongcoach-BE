package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.util.List;

/**
 * 강아지 성격 목록 조회 공개 API.
 */
public interface PersonalityFinder {

	List<Personality> findAll();
}
