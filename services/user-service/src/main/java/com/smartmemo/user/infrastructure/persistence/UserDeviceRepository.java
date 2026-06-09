package com.smartmemo.user.infrastructure.persistence;

import com.smartmemo.user.domain.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    @Query("SELECT d FROM UserDevice d WHERE d.userId = :userId AND d.deletedAt IS NULL ORDER BY d.lastOnlineAt DESC NULLS LAST")
    List<UserDevice> findByUserId(@Param("userId") UUID userId);
}
