package com.tableit.userservice.repository;

import com.tableit.userservice.model.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    List<Interest> findAllByCodeIn(Collection<String> codes);
}
