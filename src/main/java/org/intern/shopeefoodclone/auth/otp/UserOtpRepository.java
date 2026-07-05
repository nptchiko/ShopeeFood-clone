package org.intern.shopeefoodclone.auth.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOtpRepository extends JpaRepository<UserOtp, UUID> {

    Optional<UserOtp> findByEmail(String email);

    void deleteByEmail(String email);

    void deleteByExpiresAtBefore(OffsetDateTime time);
}
