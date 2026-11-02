package com.tableit.userservice.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.Objects;

// Reference data owned by Flyway migrations; never written by the application.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Immutable
@Table(name = "interests")
public class Interest {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    // Stable identifier used outside the database; ids may differ between environments.
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interest other)) return false;
        return Objects.equals(getCode(), other.getCode());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getCode());
    }

}
