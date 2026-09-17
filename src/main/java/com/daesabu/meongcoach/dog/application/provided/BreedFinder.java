package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import java.util.List;

/**
 * 강아지 견종 목록 조회 공개 API.
 */
public interface BreedFinder {

	List<Breed> findAll();
}
