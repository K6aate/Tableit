package com.tableit.userservice.controller.dto;

import com.tableit.userservice.model.Interest;

public record InterestResponse(Long id, String code, String name) {

    public static InterestResponse from(Interest interest) {
        return new InterestResponse(interest.getId(), interest.getCode(), interest.getName());
    }
}
