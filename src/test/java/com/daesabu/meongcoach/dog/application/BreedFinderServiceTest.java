package com.daesabu.meongcoach.dog.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.application.provided.BreedInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class BreedFinderServiceTest {

	private final BreedFinderService service = new BreedFinderService();

	@Test
	void 모든_견종을_코드와_한글_라벨로_반환한다() {
		List<BreedInfo> breeds = service.findAll();

		assertThat(breeds).containsExactly(
				new BreedInfo("MALTESE", "말티즈"),
				new BreedInfo("POODLE", "푸들"),
				new BreedInfo("POMERANIAN", "포메라니안"),
				new BreedInfo("BICHON_FRISE", "비숑 프리제"),
				new BreedInfo("CHIHUAHUA", "치와와"),
				new BreedInfo("SHIH_TZU", "시츄"),
				new BreedInfo("JINDO", "진돗개"),
				new BreedInfo("YORKSHIRE_TERRIER", "요크셔 테리어"),
				new BreedInfo("SPITZ", "스피츠"),
				new BreedInfo("WELSH_CORGI", "웰시 코기"),
				new BreedInfo("DACHSHUND", "닥스훈트"),
				new BreedInfo("SCHNAUZER", "슈나우저"),
				new BreedInfo("GOLDEN_RETRIEVER", "골든 리트리버"),
				new BreedInfo("LABRADOR_RETRIEVER", "래브라도 리트리버"),
				new BreedInfo("SHIBA_INU", "시바견"),
				new BreedInfo("BORDER_COLLIE", "보더 콜리"),
				new BreedInfo("PEKINGESE", "페키니즈"),
				new BreedInfo("COCKER_SPANIEL", "코카 스패니얼"),
				new BreedInfo("FRENCH_BULLDOG", "프렌치 불독"),
				new BreedInfo("PUG", "퍼그"),
				new BreedInfo("PAPILLON", "파피용"),
				new BreedInfo("SAMOYED", "사모예드"),
				new BreedInfo("SIBERIAN_HUSKY", "시베리안 허스키"),
				new BreedInfo("GERMAN_SHEPHERD", "저먼 셰퍼드"),
				new BreedInfo("BEAGLE", "비글"),
				new BreedInfo("ITALIAN_GREYHOUND", "이탈리안 그레이하운드"),
				new BreedInfo("JACK_RUSSELL_TERRIER", "잭 러셀 테리어"),
				new BreedInfo("CAVALIER_KING_CHARLES_SPANIEL", "카발리에 킹 찰스 스패니얼"),
				new BreedInfo("BOSTON_TERRIER", "보스턴 테리어"),
				new BreedInfo("DOBERMANN", "도베르만"),
				new BreedInfo("MIXED", "믹스견"));
	}
}
