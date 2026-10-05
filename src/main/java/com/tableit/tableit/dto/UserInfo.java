package com.tableit.tableit.dto;


import com.tableit.tableit.util.enums.UserRole;
import lombok.Data;

@Data
public class UserInfo {
    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String sex;
    private int universityId;
    private int facultyId;
    private int specialtyId;
    private Integer balance; // nullable
    private String telegramId; // nullable
    private Role role;
    private String avatarUrl;

    @Data
    public static class Role {
        private int level;
        private UserRole roleType;
    }

    public String getFullName(){
        return firstName + " " + lastName;
    }
}
