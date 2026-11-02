package com.tableit.userservice.service;

import com.tableit.userservice.exception.ResourceNotFoundException;
import com.tableit.userservice.model.UserProfile;
import com.tableit.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfile saveProfile(UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    public UserProfile findById(UUID userId) {
        return userProfileRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
    }
}
