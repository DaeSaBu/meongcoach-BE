package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.BreedFinder;
import com.daesabu.meongcoach.dog.application.provided.BreedInfo;
import com.daesabu.meongcoach.dog.domain.Breed;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 강아지 견종 목록을 조회한다.
 * 표시 순서는 여기서 정한다. 시트에 검색이 없어 스크롤로만 찾으므로, 첫 글자만으로 위치를 짐작할 수 있도록
 * 한글 라벨 가나다순으로 반환한다. 한글 완성형 음절은 유니코드에 초성·중성·종성 순으로 배열되어 있어
 * 라벨의 자연 순서 비교가 곧 가나다순이다.
 */
@Service
public class BreedFinderService implements BreedFinder {

	@Override
	public List<BreedInfo> findAll() {
		return Arrays.stream(Breed.values())
				.map(BreedInfo::from)
				.sorted(Comparator.comparing(BreedInfo::label))
				.toList();
	}
}
