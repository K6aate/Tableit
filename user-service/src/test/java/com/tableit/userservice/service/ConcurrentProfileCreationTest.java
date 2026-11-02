package com.tableit.userservice.service;

import com.tableit.userservice.TestcontainersConfiguration;
import com.tableit.userservice.exception.ConflictException;
import com.tableit.userservice.repository.CityRepository;
import com.tableit.userservice.security.TestJwt;
import com.tableit.userservice.service.command.CreateProfileCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduces the race deterministically: another transaction inserts the same profile but has not committed yet,
 * so createProfile's pre-check sees nothing and its insert blocks on the row lock until that transaction commits.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentProfileCreationTest {

    @DynamicPropertySource
    static void jwtKey(DynamicPropertyRegistry registry) {
        TestJwt.registerAccessPublicKey(registry);
    }

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void secondCreateOfARaceGetsConflictAndDoesNotOverwrite() throws Exception {
        UUID userId = UUID.randomUUID();
        long cityId = cityRepository.findAll().getFirst().getId();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (Connection firstRequest = dataSource.getConnection()) {
            firstRequest.setAutoCommit(false);
            insertProfileRow(firstRequest, userId, "First", cityId);

            Future<?> secondRequest = executor.submit(() -> userProfileService.createProfile(userId,
                    new CreateProfileCommand("Second", null, LocalDate.of(1995, 5, 17), cityId, null, null)));

            awaitInsertBlockedOnLock();
            firstRequest.commit();

            assertThatThrownBy(() -> secondRequest.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ConflictException.class);
        } finally {
            executor.shutdownNow();
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT first_name FROM user_schema.user_profiles WHERE user_id = ?", String.class, userId))
                .isEqualTo("First");
    }

    private static void insertProfileRow(Connection connection, UUID userId, String firstName, long cityId) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO user_schema.user_profiles (user_id, first_name, date_of_birth, city_id) VALUES (?, ?, ?, ?)")) {
            insert.setObject(1, userId);
            insert.setString(2, firstName);
            insert.setDate(3, Date.valueOf(LocalDate.of(1990, 1, 1)));
            insert.setLong(4, cityId);
            insert.executeUpdate();
        }
    }

    // Polls PostgreSQL until the service's INSERT is waiting on the uncommitted row's lock.
    private void awaitInsertBlockedOnLock() throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            Integer waiting = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND query ILIKE 'insert into user_schema.user_profiles%'",
                    Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("createProfile's insert never blocked on the concurrent row lock");
    }
}
