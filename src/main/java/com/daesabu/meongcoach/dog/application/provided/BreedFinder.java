package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import java.util.List;

/**
 * 강아지 견종 목록 조회 공개 API. 선언 순서(온보딩 표시 순)대로 반환한다.
 */
public interface BreedFinder {

	List<Breed> findAll();
}
