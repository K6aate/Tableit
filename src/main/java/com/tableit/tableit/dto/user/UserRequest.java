package com.tableit.tableit.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tableit.tableit.util.enums.Sex;
import lombok.Data;

@Data
public class UserRequest {

    private String firstName;

    private String lastName;

    private Sex sex;

    private String aboutMe;

    private Long universityId;

    private Long facultyId;

    @JsonProperty("specialityId")
    private Long specialityId;
}
