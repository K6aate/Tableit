package com.tableit.userservice.controller.dto;

import com.tableit.userservice.model.City;

public record CityResponse(Long id, String name, String countryCode) {

    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getCountryCode());
    }
}
