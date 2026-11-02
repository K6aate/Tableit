package com.tableit.userservice.service;

import com.tableit.userservice.TestcontainersConfiguration;
import com.tableit.userservice.exception.BadRequestException;
import com.tableit.userservice.exception.BaseApiException;
import com.tableit.userservice.exception.ConflictException;
import com.tableit.userservice.exception.ResourceNotFoundException;
import com.tableit.userservice.model.Interest;
import com.tableit.userservice.model.UserProfile;
import com.tableit.userservice.repository.CityRepository;
import com.tableit.userservice.service.command.CreateProfileCommand;
import com.tableit.userservice.service.command.UpdateProfileCommand;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs without a test transaction so the service's own transactions and fetch plans are exercised as in production.
@SpringBootTest
@Import({TestcontainersConfiguration.class, UserProfileServiceTest.FixedClockConfiguration.class})
class UserProfileServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private SettableClock clock;

    private long bratislavaId;

    @BeforeEach
    void setUp() {
        clock.setDate(TODAY);
        bratislavaId = cityRepository.findAll().stream()
                .filter(city -> city.getName().equals("Bratislava"))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    // --- createProfile -------------------------------------------------------------------------

    @Test
    void createsProfile() {
        UUID userId = UUID.randomUUID();

        userProfileService.createProfile(userId, new CreateProfileCommand(
                "Ada", "Lovelace", LocalDate.of(1995, 5, 17), bratislavaId,
                "https://example.com/ada.png", "Likes hiking"));

        UserProfile stored = userProfileService.getProfile(userId);
        assertThat(stored.getFirstName()).isEqualTo("Ada");
        assertThat(stored.getLastName()).isEqualTo("Lovelace");
        assertThat(stored.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 17));
        assertThat(stored.getCity().getName()).isEqualTo("Bratislava");
        assertThat(stored.getAvatarUrl()).isEqualTo("https://example.com/ada.png");
        assertThat(stored.getBio()).isEqualTo("Likes hiking");
        assertThat(stored.getInterests()).isEmpty();
    }

    @Test
    void rejectsDuplicateProfileWithConflict() {
        UUID userId = createDefaultProfile();

        assertThatThrownBy(() -> userProfileService.createProfile(userId, validCommand()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((BaseApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    // --- age -----------------------------------------------------------------------------------

    @Test
    void rejectsUserOneDayYoungerThan18() {
        LocalDate seventeenYears364Days = TODAY.minusYears(18).plusDays(1);

        assertThatThrownBy(() -> createWithDateOfBirth(seventeenYears364Days))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 18");
    }

    @Test
    void acceptsUserExactly18() {
        UUID userId = createWithDateOfBirth(TODAY.minusYears(18));

        assertThat(userProfileService.getProfile(userId).getDateOfBirth()).isEqualTo(TODAY.minusYears(18));
    }

    @Test
    void leapDayBirthdayTurns18OnFirstOfMarchInNonLeapYear() {
        LocalDate leapDay = LocalDate.of(2008, 2, 29);

        clock.setDate(LocalDate.of(2026, 2, 28));
        assertThatThrownBy(() -> createWithDateOfBirth(leapDay))
                .isInstanceOf(BadRequestException.class);

        clock.setDate(LocalDate.of(2026, 3, 1));
        UUID userId = createWithDateOfBirth(leapDay);
        assertThat(userProfileService.getProfile(userId).getDateOfBirth()).isEqualTo(leapDay);
    }

    @Test
    void rejectsAge121() {
        assertThatThrownBy(() -> createWithDateOfBirth(TODAY.minusYears(121)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("120");
    }

    // --- city and normalization ----------------------------------------------------------------

    @Test
    void rejectsUnknownCity() {
        assertThatThrownBy(() -> userProfileService.createProfile(UUID.randomUUID(), new CreateProfileCommand(
                "Ada", null, LocalDate.of(1995, 5, 17), -1L, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown city");
    }

    @Test
    void trimsFirstName() {
        UUID userId = UUID.randomUUID();

        userProfileService.createProfile(userId, new CreateProfileCommand(
                "  Ada  ", null, LocalDate.of(1995, 5, 17), bratislavaId, null, null));

        assertThat(userProfileService.getProfile(userId).getFirstName()).isEqualTo("Ada");
    }

    @Test
    void rejectsBlankFirstName() {
        assertThatThrownBy(() -> userProfileService.createProfile(UUID.randomUUID(), new CreateProfileCommand(
                "   ", null, LocalDate.of(1995, 5, 17), bratislavaId, null, null)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void storesBlankOptionalFieldsAsNull() {
        UUID userId = UUID.randomUUID();

        userProfileService.createProfile(userId, new CreateProfileCommand(
                "Ada", "   ", LocalDate.of(1995, 5, 17), bratislavaId, "", "   "));

        UserProfile stored = userProfileService.getProfile(userId);
        assertThat(stored.getLastName()).isNull();
        assertThat(stored.getAvatarUrl()).isNull();
        assertThat(stored.getBio()).isNull();
    }

    @Test
    void storesWhitespaceOnlyAvatarUrlAsNull() {
        UUID userId = UUID.randomUUID();

        userProfileService.createProfile(userId, new CreateProfileCommand(
                "Ada", null, LocalDate.of(1995, 5, 17), bratislavaId, "   ", null));

        assertThat(userProfileService.getProfile(userId).getAvatarUrl()).isNull();
    }

    // --- interests -----------------------------------------------------------------------------

    @Test
    void rejectsTwoInterests() {
        UUID userId = createDefaultProfile();

        assertThatThrownBy(() -> userProfileService.replaceInterests(userId, List.of("hiking", "books")))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void acceptsThreeInterests() {
        UUID userId = createDefaultProfile();

        userProfileService.replaceInterests(userId, List.of("hiking", "books", "coffee"));

        assertThat(interestCodes(userId)).containsExactlyInAnyOrder("hiking", "books", "coffee");
    }

    @Test
    void acceptsTenInterests() {
        UUID userId = createDefaultProfile();
        List<String> ten = List.of("board_games", "movies", "hiking", "books", "sports",
                "music", "travel", "cooking", "art", "coffee");

        userProfileService.replaceInterests(userId, ten);

        assertThat(interestCodes(userId)).containsExactlyInAnyOrderElementsOf(ten);
    }

    @Test
    void rejectsElevenInterests() {
        UUID userId = createDefaultProfile();
        List<String> eleven = List.of("board_games", "movies", "hiking", "books", "sports",
                "music", "travel", "cooking", "art", "coffee", "photography");

        assertThatThrownBy(() -> userProfileService.replaceInterests(userId, eleven))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsDuplicateInterestCodes() {
        UUID userId = createDefaultProfile();

        assertThatThrownBy(() -> userProfileService.replaceInterests(userId, List.of("hiking", "books", "hiking")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate interest codes: hiking");
    }

    @Test
    void rejectsAndListsUnknownInterestCodes() {
        UUID userId = createDefaultProfile();

        assertThatThrownBy(() -> userProfileService.replaceInterests(userId, List.of("hiking", "skydiving", "chess")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown interest codes: chess, skydiving");
    }

    @Test
    void replacingInterestsReplacesWholeSet() {
        UUID userId = createDefaultProfile();
        userProfileService.replaceInterests(userId, List.of("hiking", "books", "coffee"));

        userProfileService.replaceInterests(userId, List.of("music", "art", "travel"));

        assertThat(interestCodes(userId)).containsExactlyInAnyOrder("music", "art", "travel");
    }

    // --- missing profile -----------------------------------------------------------------------

    @Test
    void updateOfMissingProfileIsNotFound() {
        assertThatThrownBy(() -> userProfileService.updateProfile(UUID.randomUUID(), new UpdateProfileCommand(
                "Ada", null, LocalDate.of(1995, 5, 17), bratislavaId, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(e -> ((BaseApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOfMissingProfileIsNotFound() {
        assertThatThrownBy(() -> userProfileService.getProfile(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(e -> ((BaseApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- helpers -------------------------------------------------------------------------------

    private CreateProfileCommand validCommand() {
        return new CreateProfileCommand("Ada", null, LocalDate.of(1995, 5, 17), bratislavaId, null, null);
    }

    private UUID createDefaultProfile() {
        UUID userId = UUID.randomUUID();
        userProfileService.createProfile(userId, validCommand());
        return userId;
    }

    private UUID createWithDateOfBirth(LocalDate dateOfBirth) {
        UUID userId = UUID.randomUUID();
        userProfileService.createProfile(userId, new CreateProfileCommand(
                "Ada", null, dateOfBirth, bratislavaId, null, null));
        return userId;
    }

    private List<String> interestCodes(UUID userId) {
        return userProfileService.getProfile(userId).getInterests().stream()
                .map(Interest::getCode)
                .toList();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        SettableClock settableClock() {
            return new SettableClock();
        }
    }

    // Fixed clock whose date a test can move, e.g. to check a birthday boundary.
    static class SettableClock extends Clock {
        private Instant instant = Instant.EPOCH;

        void setDate(LocalDate date) {
            instant = date.atStartOfDay(ZoneOffset.UTC).plusHours(12).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
