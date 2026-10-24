package com.daesabu.meongcoach.dog.application.provided;

/**
 * 강아지 프로필 삭제 능력. 행을 지우지 않는 소프트 딜리트다.
 */
public interface DogProfileDeleter {

	/**
	 * 사용자가 소유한 미선택 강아지를 삭제하고 이후 조회에서 제외한다.
	 * 없거나 본인 소유가 아니거나 이미 삭제됐으면 {@code DogNotFoundException}을 던진다.
	 * 사용자의 강아지가 한 마리뿐이면 {@code LastDogNotDeletableException}을 던지고 삭제하지 않는다.
	 * 선택된 강아지이면 {@code SelectedDogNotDeletableException}을 던지고 삭제하지 않는다.
	 */
	void delete(Long userId, Long dogId);
}
