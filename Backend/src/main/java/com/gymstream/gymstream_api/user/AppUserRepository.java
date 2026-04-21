package com.gymstream.gymstream_api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findBySessionToken(String sessionToken);
}