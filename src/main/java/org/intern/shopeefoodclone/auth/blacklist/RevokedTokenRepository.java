package org.intern.shopeefoodclone.auth.blacklist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {

    boolean existsByJti(String jti);

    void deleteByExpiresAtBefore(OffsetDateTime time);

    void deleteByUserId(UUID userId);
}
