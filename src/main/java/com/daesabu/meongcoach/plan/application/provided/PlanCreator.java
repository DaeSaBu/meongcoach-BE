package com.daesabu.meongcoach.plan.application.provided;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import jakarta.validation.Valid;

/**
 * 맞춤 플랜 생성 능력.
 */
public interface PlanCreator {

	/**
	 * 요청의 방식(AI, MANUAL)에 맞춰 플랜을 만들어 저장하고 현재 플랜 상세를 반환한다.
	 * 진행 중인 플랜이 있고 교체를 요청하지 않으면 {@code PlanAlreadyActiveException}을 던지고,
	 * 교체를 요청하면 기존 플랜을 포기 처리한다.
	 * 진행 중인 플랜의 강아지가 삭제됐으면 교체 요청과 관계없이 그 플랜을 포기 처리하고 새로 만든다.
	 * 방식별 필수값이 없거나 토픽이 없으면 400 계열 예외를, 고른 토픽에 열린 커리큘럼이 없으면
	 * {@code PlanCatalogEmptyException}을 던진다. 기대사항이 있으면 강아지 프로필에도 반영한다.
	 */
	PlanDetailResult create(Long userId, @Valid PlanCreateRequest request);
}
