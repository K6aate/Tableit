package com.tableit.userservice.service.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

// Replaces all editable profile fields; interests are changed separately.
// Shape rules only; age, city existence and trimming are enforced by the service.
public record UpdateProfileCommand(
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Long cityId,
        @URL(regexp = "^(?i)https?://.*") @Size(max = 2048) String avatarUrl,
        @Size(max = 1000) String bio
) {
    // Runs before validation, so a cleared avatar URL is treated as absent rather than as an invalid URL.
    public UpdateProfileCommand {
        avatarUrl = avatarUrl == null || avatarUrl.isBlank() ? null : avatarUrl.strip();
    }
}
