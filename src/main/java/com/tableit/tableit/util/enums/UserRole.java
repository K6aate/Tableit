package com.tableit.tableit.util.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public enum UserRole {

    USER(1),
    MANAGER(2),
    ADMIN(3);

    @Getter
    private final int level;

    private static final List<UserRole> USER_ROLE_LIST =
            Arrays.stream(UserRole.values())
                    .sorted(Comparator.comparingInt(UserRole::getLevel))
                    .toList();


    public static List<UserRole> getRolesSortedByLevel() {
        return USER_ROLE_LIST;
    }

    public String getName() {
        return this.name();
    }
}
