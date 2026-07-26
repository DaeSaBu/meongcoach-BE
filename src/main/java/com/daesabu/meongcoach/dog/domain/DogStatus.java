package com.daesabu.meongcoach.dog.domain;

/**
 * 강아지 상태. ERD상 현재 선택(표시) 여부를 뜻하는 boolean 성격의 컬럼(DEFAULT TRUE)이라 SELECTED/UNSELECTED로 정정해 정의했다 — 기획 확인 필요.
 */
public enum DogStatus {
	SELECTED,
	UNSELECTED,
}
