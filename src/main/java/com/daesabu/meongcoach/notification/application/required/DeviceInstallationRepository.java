package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface DeviceInstallationRepository extends JpaRepository<DeviceInstallation, Long> {

	Optional<DeviceInstallation> findByFid(String fid);

	List<DeviceInstallation> findAllByUserIdInOrderByUserIdAscIdAsc(Collection<Long> userIds);

	void deleteByUserIdAndFid(Long userId, String fid);

	/**
	 * 무효 FID를 별도 트랜잭션에서 삭제한다. 호출한 쪽의 트랜잭션이 롤백돼도 삭제는 유지된다.
	 */
	@Modifying
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Query("delete from DeviceInstallation d where d.fid in :fids")
	void deleteAllByFidIn(@Param("fids") Collection<String> fids);

	void deleteAllByUserId(Long userId);
}
