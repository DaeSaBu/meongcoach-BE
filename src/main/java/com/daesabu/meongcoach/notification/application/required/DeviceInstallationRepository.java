package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceInstallationRepository extends JpaRepository<DeviceInstallation, Long> {

	Optional<DeviceInstallation> findByFid(String fid);

	List<DeviceInstallation> findAllByUserIdInOrderByUserIdAscIdAsc(Collection<Long> userIds);

	void deleteByUserIdAndFid(Long userId, String fid);

	@Modifying
	@Query("delete from DeviceInstallation d where d.fid in :fids")
	void deleteAllByFidIn(@Param("fids") Collection<String> fids);

	void deleteAllByUserId(Long userId);
}
