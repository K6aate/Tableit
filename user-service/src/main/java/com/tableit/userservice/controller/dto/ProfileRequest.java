package com.tableit.userservice.controller.dto;

import com.tableit.userservice.service.command.CreateProfileCommand;
import com.tableit.userservice.service.command.UpdateProfileCommand;

import java.time.LocalDate;

// Body of POST and PUT /api/users/me/profile. Deliberately has no userId: identity comes only from the JWT.
// Validation rules live in the command records, so they are defined once and checked by the service.
public record ProfileRequest(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Long cityId,
        String avatarUrl,
        String bio
) {

    public CreateProfileCommand toCreateCommand() {
        return new CreateProfileCommand(firstName, lastName, dateOfBirth, cityId, avatarUrl, bio);
    }

    public UpdateProfileCommand toUpdateCommand() {
        return new UpdateProfileCommand(firstName, lastName, dateOfBirth, cityId, avatarUrl, bio);
    }
}
