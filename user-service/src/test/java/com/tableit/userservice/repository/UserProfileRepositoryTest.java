package com.tableit.userservice.repository;

import com.tableit.userservice.TestcontainersConfiguration;
import com.tableit.userservice.model.UserProfile;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserProfileRepositoryTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storesProfileUnderUuidSuppliedByCaller() {
        UUID userId = UUID.randomUUID();

        userProfileRepository.saveAndFlush(new UserProfile().setUserId(userId).setFirstName("Ada"));
        entityManager.clear();

        UserProfile stored = userProfileRepository.findById(userId).orElseThrow();
        assertThat(stored.getFirstName()).isEqualTo("Ada");
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }
}
