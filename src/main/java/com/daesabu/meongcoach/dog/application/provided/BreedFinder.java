package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import java.util.List;

/**
 * 강아지 견종 목록 조회 공개 API.
 */
public interface BreedFinder {

	/**
	 * 등록 견종 전체를 화면에 표시하는 순서로 반환한다.
	 */
	List<RegisteredBreed> findAll();
}
