package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.exception.InvalidBreedException;

/**
 * 강아지 견종.
 * 선언 순서가 곧 온보딩 목록의 표시 순서다. 국내 양육 비중 순(KB 2025 반려동물보고서 → 농림축산검역본부
 * 동물등록 상위 품종 → 그 외 국내에서 흔한 견종)으로 두고, 믹스견은 항상 마지막에 둔다.
 * 코드는 30자를 넘지 않아야 한다(dogs.breed 컬럼 길이·온보딩 요청 검증 상한).
 */
public enum Breed {
	MALTESE("말티즈"),
	POODLE("푸들"),
	POMERANIAN("포메라니안"),
	BICHON_FRISE("비숑 프리제"),
	CHIHUAHUA("치와와"),
	SHIH_TZU("시츄"),
	JINDO("진돗개"),
	YORKSHIRE_TERRIER("요크셔 테리어"),
	SPITZ("스피츠"),
	WELSH_CORGI("웰시 코기"),
	DACHSHUND("닥스훈트"),
	SCHNAUZER("슈나우저"),
	GOLDEN_RETRIEVER("골든 리트리버"),
	LABRADOR_RETRIEVER("래브라도 리트리버"),
	SHIBA_INU("시바견"),
	BORDER_COLLIE("보더 콜리"),
	PEKINGESE("페키니즈"),
	COCKER_SPANIEL("코카 스패니얼"),
	FRENCH_BULLDOG("프렌치 불독"),
	PUG("퍼그"),
	PAPILLON("파피용"),
	SAMOYED("사모예드"),
	SIBERIAN_HUSKY("시베리안 허스키"),
	GERMAN_SHEPHERD("저먼 셰퍼드"),
	BEAGLE("비글"),
	ITALIAN_GREYHOUND("이탈리안 그레이하운드"),
	JACK_RUSSELL_TERRIER("잭 러셀 테리어"),
	CAVALIER_KING_CHARLES_SPANIEL("카발리에 킹 찰스 스패니얼"),
	BOSTON_TERRIER("보스턴 테리어"),
	DOBERMANN("도베르만"),
	MIXED("믹스견"),
	;

	private final String label;

	Breed(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * 문자열 코드를 enum으로 변환한다.
	 * 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
	public static Breed from(String value) {
		if (value == null) {
			throw new InvalidBreedException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidBreedException(value);
		}
	}
}
