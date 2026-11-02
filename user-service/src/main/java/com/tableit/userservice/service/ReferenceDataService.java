package com.tableit.userservice.service;

import com.tableit.userservice.model.City;
import com.tableit.userservice.model.Interest;
import com.tableit.userservice.repository.CityRepository;
import com.tableit.userservice.repository.InterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Read-only access to the migration-managed reference data used during onboarding.
@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final InterestRepository interestRepository;
    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<Interest> interests() {
        return interestRepository.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public List<City> cities() {
        return cityRepository.findAll(Sort.by("name"));
    }
}
