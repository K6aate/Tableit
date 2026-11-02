package com.tableit.userservice.repository;

import com.tableit.userservice.model.UserProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    // Loads the lazy associations up front; open-in-view is disabled.
    @EntityGraph(attributePaths = {"city", "interests"})
    Optional<UserProfile> findWithDetailsByUserId(UUID userId);
}
