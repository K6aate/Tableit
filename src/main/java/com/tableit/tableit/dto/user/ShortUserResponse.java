package com.tableit.tableit.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortUserResponse {
    private String username;
    private String fullName;
}
