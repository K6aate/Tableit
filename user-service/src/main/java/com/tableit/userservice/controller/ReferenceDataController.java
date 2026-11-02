package com.tableit.userservice.controller;

import com.tableit.userservice.controller.dto.CityResponse;
import com.tableit.userservice.controller.dto.InterestResponse;
import com.tableit.userservice.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Choices offered during onboarding. Authenticated, because onboarding happens after registration.
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/interests")
    public List<InterestResponse> interests() {
        return referenceDataService.interests().stream().map(InterestResponse::from).toList();
    }

    @GetMapping("/cities")
    public List<CityResponse> cities() {
        return referenceDataService.cities().stream().map(CityResponse::from).toList();
    }
}
