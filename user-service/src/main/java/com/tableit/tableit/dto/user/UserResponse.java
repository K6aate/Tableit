package com.tableit.tableit.dto.user;


import com.tableit.tableit.util.enums.Sex;
import com.tableit.tableit.util.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;

    private String username;
    private UserRole role;
    private String firstName;
    private String lastName;

    private Sex sex;

    private Long telegramId;

    private String avatarUrl;
    private String aboutMe;

}
