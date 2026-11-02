package com.tableit.userservice.controller.dto;

import com.tableit.userservice.model.UserProfile;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String firstName,
        String lastName,
        String avatarUrl,
        String bio,
        LocalDate dateOfBirth,
        CityResponse city,
        List<InterestResponse> interests
) {

    // Expects city and interests to be loaded (see UserProfileService.getProfile).
    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.getUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getAvatarUrl(),
                profile.getBio(),
                profile.getDateOfBirth(),
                CityResponse.from(profile.getCity()),
                profile.getInterests().stream()
                        .map(InterestResponse::from)
                        .sorted(Comparator.comparing(InterestResponse::code))
                        .toList());
    }
}
