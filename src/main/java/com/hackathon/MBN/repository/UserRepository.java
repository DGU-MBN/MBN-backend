package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByDeviceToken(String deviceToken);
}
