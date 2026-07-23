package com.daesabu.meongcoach.training.domain;

/**
 * 커리큘럼 노출 대상 강아지 크기. null이면 전체 노출이다. dog 모듈의 DogSize와 별개 개념이라 모듈 내에 따로 정의한다(모듈 간 domain 타입 참조 금지).
 */
public enum TargetDogSize {
	SMALL,
	MEDIUM,
	LARGE,
}
