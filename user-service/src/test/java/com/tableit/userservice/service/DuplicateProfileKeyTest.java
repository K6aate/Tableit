package com.tableit.userservice.service;

import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

// Only the profile primary-key violation may become a 409; every other database error must stay visible.
class DuplicateProfileKeyTest {

    @Test
    void recognisesProfilePrimaryKeyViolation() {
        assertThat(UserProfileService.isDuplicateProfileKey(violation("23505", "user_profiles_pkey"))).isTrue();
    }

    @Test
    void recognisesItWhenWrappedInPersistenceException() {
        PersistenceException wrapped = new PersistenceException("flush failed", violation("23505", "user_profiles_pkey"));

        assertThat(UserProfileService.isDuplicateProfileKey(wrapped)).isTrue();
    }

    @Test
    void ignoresUniqueViolationOfAnotherConstraint() {
        assertThat(UserProfileService.isDuplicateProfileKey(violation("23505", "interests_code_key"))).isFalse();
    }

    @Test
    void ignoresOtherKindsOfConstraintViolation() {
        assertThat(UserProfileService.isDuplicateProfileKey(
                violation("23514", "ck_user_profiles_first_name_not_blank"))).isFalse();
        assertThat(UserProfileService.isDuplicateProfileKey(
                violation("23503", "user_profiles_city_id_fkey"))).isFalse();
    }

    @Test
    void ignoresUnrelatedErrors() {
        assertThat(UserProfileService.isDuplicateProfileKey(new PersistenceException("connection lost"))).isFalse();
    }

    private static ConstraintViolationException violation(String sqlState, String constraintName) {
        return new ConstraintViolationException("constraint violated", new SQLException("constraint violated", sqlState), constraintName);
    }
}
