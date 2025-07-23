package com.virtualbank.user.repository;

import com.virtualbank.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsernameOrEmail(String username, String email);

    Optional<User> findByUsername(String username);


    Optional<User> findByEmail(String email);
}