package com.daesabu.meongcoach.dog.application.provided;

/**
 * 강아지 프로필 삭제 능력. 행을 지우지 않는 소프트 딜리트다.
 */
public interface DogProfileDeleter {

	/**
	 * 사용자가 소유한 강아지를 삭제한다. 이후 조회에서 제외되며 선택 상태는 다른 강아지로 넘어가지 않는다.
	 * 없거나 본인 소유가 아니거나 이미 삭제됐으면 {@code DogNotFoundException}을 던진다.
	 */
	void delete(Long userId, Long dogId);
}
