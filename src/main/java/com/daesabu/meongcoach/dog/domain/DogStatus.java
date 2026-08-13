package com.daesabu.meongcoach.dog.domain;

/**
 * 강아지 상태. ERD상 현재 선택(표시) 여부를 뜻하는 boolean 성격의 컬럼이라 SELECTED/UNSELECTED로 정정해 정의했다.
 * 사용자당 SELECTED는 한 마리이며, 선택된 강아지가 없을 때 등록한 첫 강아지가 선택된다.
 * 사용자별 SELECTED 한 건은 부분 유니크 인덱스라 JPA로 표현할 수 없어, DB 제약 대신 DogRegisterService에서 강제한다.
 */
public enum DogStatus {
	SELECTED,
	UNSELECTED,
}
