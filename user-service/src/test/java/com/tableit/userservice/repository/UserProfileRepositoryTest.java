package com.tableit.userservice.repository;

import com.tableit.userservice.TestcontainersConfiguration;
import com.tableit.userservice.model.City;
import com.tableit.userservice.model.Interest;
import com.tableit.userservice.model.UserProfile;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserProfileRepositoryTest {

    private static final List<String> SEEDED_INTEREST_CODES = List.of(
            "board_games", "movies", "hiking", "books", "sports", "music",
            "travel", "cooking", "art", "coffee", "photography", "languages");

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private EntityManager entityManager;

    // Constraint tests use plain SQL so they exercise the database, not Hibernate's own checks.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void storesProfileUnderUuidSuppliedByCaller() {
        UUID userId = UUID.randomUUID();

        userProfileRepository.saveAndFlush(newProfile(userId));
        entityManager.clear();

        UserProfile stored = userProfileRepository.findById(userId).orElseThrow();
        assertThat(stored.getFirstName()).isEqualTo("Ada");
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }

    @Test
    void seedsBratislavaAndAllInterests() {
        assertThat(cityRepository.findAll())
                .extracting(City::getName, City::getCountryCode)
                .contains(tuple("Bratislava", "SK"));

        assertThat(interestRepository.findAll())
                .extracting(Interest::getCode)
                .containsAll(SEEDED_INTEREST_CODES);
    }

    @Test
    void loadsProfileWithCityAndInterests() {
        UUID userId = UUID.randomUUID();
        List<Interest> chosen = interestRepository.findAllByCodeIn(List.of("hiking", "books", "coffee"));

        userProfileRepository.saveAndFlush(newProfile(userId).replaceInterests(new HashSet<>(chosen)));
        entityManager.clear();

        UserProfile stored = userProfileRepository.findWithDetailsByUserId(userId).orElseThrow();
        // Detach before reading the associations: they must already be loaded by the entity graph.
        entityManager.clear();

        assertThat(stored.getCity().getName()).isEqualTo("Bratislava");
        assertThat(stored.getInterests())
                .extracting(Interest::getCode)
                .containsExactlyInAnyOrder("hiking", "books", "coffee");
    }

    @Test
    void acceptsMissingLastName() {
        UUID userId = UUID.randomUUID();

        userProfileRepository.saveAndFlush(newProfile(userId).setLastName(null));
        entityManager.clear();

        assertThat(userProfileRepository.findById(userId).orElseThrow().getLastName()).isNull();
    }

    @Test
    void rejectsDuplicateUserInterest() {
        UUID userId = insertProfileRow();
        long interestId = interestId("music");
        insertUserInterest(userId, interestId);

        assertThatThrownBy(() -> insertUserInterest(userId, interestId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsUnknownInterest() {
        UUID userId = insertProfileRow();

        assertThatThrownBy(() -> insertUserInterest(userId, -1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMissingFirstName() {
        assertThatThrownBy(() -> insertProfileRow(null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsBlankFirstName() {
        assertThatThrownBy(() -> insertProfileRow("   "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingProfileRemovesInterestLinksButKeepsInterests() {
        UUID userId = UUID.randomUUID();
        List<Interest> chosen = interestRepository.findAllByCodeIn(List.of("art", "travel", "movies"));
        userProfileRepository.saveAndFlush(newProfile(userId).replaceInterests(new HashSet<>(chosen)));
        assertThat(countUserInterests(userId)).isEqualTo(3);

        userProfileRepository.deleteById(userId);
        userProfileRepository.flush();

        assertThat(countUserInterests(userId)).isZero();
        assertThat(interestRepository.findAllByCodeIn(List.of("art", "travel", "movies"))).hasSize(3);
    }

    @Test
    void databaseCascadesInterestLinksWhenProfileRowIsDeleted() {
        UUID userId = insertProfileRow();
        insertUserInterest(userId, interestId("cooking"));

        jdbcTemplate.update("DELETE FROM user_schema.user_profiles WHERE user_id = ?", userId);

        assertThat(countUserInterests(userId)).isZero();
        assertThat(interestRepository.findAllByCodeIn(List.of("cooking"))).hasSize(1);
    }

    private UserProfile newProfile(UUID userId) {
        return new UserProfile()
                .setUserId(userId)
                .setFirstName("Ada")
                .setDateOfBirth(LocalDate.of(1995, 5, 17))
                .setCity(bratislava());
    }

    private City bratislava() {
        return cityRepository.findAll().stream()
                .filter(city -> city.getName().equals("Bratislava"))
                .findFirst()
                .orElseThrow();
    }

    private UUID insertProfileRow() {
        return insertProfileRow("Ada");
    }

    private UUID insertProfileRow(String firstName) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO user_schema.user_profiles (user_id, first_name, date_of_birth, city_id) VALUES (?, ?, ?, ?)",
                userId, firstName, LocalDate.of(1995, 5, 17), bratislava().getId());
        return userId;
    }

    private void insertUserInterest(UUID userId, long interestId) {
        jdbcTemplate.update(
                "INSERT INTO user_schema.user_interests (user_id, interest_id) VALUES (?, ?)",
                userId, interestId);
    }

    private long interestId(String code) {
        return interestRepository.findAllByCodeIn(List.of(code)).getFirst().getId();
    }

    private int countUserInterests(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_schema.user_interests WHERE user_id = ?", Integer.class, userId);
    }
}
