package com.tableit.userservice.controller;

import com.tableit.userservice.controller.dto.InterestsRequest;
import com.tableit.userservice.controller.dto.ProfileRequest;
import com.tableit.userservice.controller.dto.ProfileResponse;
import com.tableit.userservice.security.CurrentUserId;
import com.tableit.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

// The caller's own profile. The user id always comes from the access token, never from the request.
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ProfileResponse getMyProfile(@CurrentUserId UUID userId) {
        return currentProfile(userId);
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileResponse> createMyProfile(@CurrentUserId UUID userId,
                                                           @RequestBody ProfileRequest request) {
        userProfileService.createProfile(userId, request.toCreateCommand());
        return ResponseEntity.created(URI.create("/api/users/me")).body(currentProfile(userId));
    }

    @PutMapping("/profile")
    public ProfileResponse updateMyProfile(@CurrentUserId UUID userId, @RequestBody ProfileRequest request) {
        userProfileService.updateProfile(userId, request.toUpdateCommand());
        return currentProfile(userId);
    }

    @PutMapping("/interests")
    public ProfileResponse replaceMyInterests(@CurrentUserId UUID userId, @RequestBody InterestsRequest request) {
        userProfileService.replaceInterests(userId, request.codes());
        return currentProfile(userId);
    }

    // Re-reads with city and interests loaded, so the response never touches lazy state after a write.
    private ProfileResponse currentProfile(UUID userId) {
        return ProfileResponse.from(userProfileService.getProfile(userId));
    }
}
