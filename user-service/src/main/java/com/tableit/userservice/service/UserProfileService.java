package com.tableit.userservice.service;

import com.tableit.userservice.exception.BadRequestException;
import com.tableit.userservice.exception.ConflictException;
import com.tableit.userservice.exception.ResourceNotFoundException;
import com.tableit.userservice.model.City;
import com.tableit.userservice.model.Interest;
import com.tableit.userservice.model.UserProfile;
import com.tableit.userservice.repository.CityRepository;
import com.tableit.userservice.repository.InterestRepository;
import com.tableit.userservice.repository.UserProfileRepository;
import com.tableit.userservice.service.command.CreateProfileCommand;
import com.tableit.userservice.service.command.UpdateProfileCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Validated
@Service
@RequiredArgsConstructor
public class UserProfileService {

    static final int MIN_AGE = 18;
    static final int MAX_AGE = 120;
    static final int MIN_INTERESTS = 3;
    static final int MAX_INTERESTS = 10;

    private static final String PROFILE_EXISTS = "User profile already exists";
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";
    private static final String PROFILE_PRIMARY_KEY = "user_profiles_pkey";

    private final EntityManager entityManager;
    private final UserProfileRepository userProfileRepository;
    private final CityRepository cityRepository;
    private final InterestRepository interestRepository;
    private final Clock clock;

    @Transactional
    public UserProfile createProfile(@NotNull UUID userId, @NotNull @Valid CreateProfileCommand command) {
        if (userProfileRepository.existsById(userId)) {
            throw new ConflictException(PROFILE_EXISTS);
        }

        UserProfile profile = new UserProfile().setUserId(userId);
        applyDetails(profile, command.firstName(), command.lastName(), command.dateOfBirth(),
                command.cityId(), command.avatarUrl(), command.bio());

        // persist, not save(): save() merges entities with an assigned id, so a profile committed by a concurrent
        // request after the check above would be silently overwritten. Flushing here surfaces the duplicate key
        // inside this method instead of at commit time.
        try {
            entityManager.persist(profile);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (isDuplicateProfileKey(e)) {
                throw new ConflictException(PROFILE_EXISTS);
            }
            throw e;
        }
        return profile;
    }

    @Transactional
    public UserProfile updateProfile(@NotNull UUID userId, @NotNull @Valid UpdateProfileCommand command) {
        UserProfile profile = findProfile(userId);
        applyDetails(profile, command.firstName(), command.lastName(), command.dateOfBirth(),
                command.cityId(), command.avatarUrl(), command.bio());
        return profile;
    }

    @Transactional
    public UserProfile replaceInterests(
            @NotNull UUID userId,
            @NotNull @Size(min = MIN_INTERESTS, max = MAX_INTERESTS) List<@NotBlank String> codes) {
        UserProfile profile = findProfile(userId);

        Set<String> duplicates = codes.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> entry.getKey())
                .collect(Collectors.toCollection(TreeSet::new));
        if (!duplicates.isEmpty()) {
            throw new BadRequestException("Duplicate interest codes: " + String.join(", ", duplicates));
        }

        List<Interest> interests = interestRepository.findAllByCodeIn(codes);
        Set<String> unknown = new TreeSet<>(codes);
        interests.forEach(interest -> unknown.remove(interest.getCode()));
        if (!unknown.isEmpty()) {
            throw new BadRequestException("Unknown interest codes: " + String.join(", ", unknown));
        }

        return profile.replaceInterests(new HashSet<>(interests));
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(@NotNull UUID userId) {
        return userProfileRepository.findWithDetailsByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
    }

    private UserProfile findProfile(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
    }

    private void applyDetails(UserProfile profile, String firstName, String lastName, LocalDate dateOfBirth,
                              Long cityId, String avatarUrl, String bio) {
        validateAge(dateOfBirth);

        profile.setFirstName(firstName.strip())
                .setLastName(stripToNull(lastName))
                .setDateOfBirth(dateOfBirth)
                .setCity(findCity(cityId))
                .setAvatarUrl(stripToNull(avatarUrl))
                .setBio(stripToNull(bio));
    }

    private void validateAge(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now(clock)).getYears();
        if (age < MIN_AGE) {
            throw new BadRequestException("User must be at least " + MIN_AGE + " years old");
        }
        if (age > MAX_AGE) {
            throw new BadRequestException("Age must not exceed " + MAX_AGE + " years");
        }
    }

    private City findCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new BadRequestException("Unknown city: " + cityId));
    }

    // Only a unique violation of the profile primary key means "already exists"; any other error is rethrown.
    static boolean isDuplicateProfileKey(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                return UNIQUE_VIOLATION_SQL_STATE.equals(violation.getSQLState())
                        && PROFILE_PRIMARY_KEY.equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
