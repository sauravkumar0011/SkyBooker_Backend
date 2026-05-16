package com.skybooker.airline.repository;

import com.skybooker.airline.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AirlineRepository extends JpaRepository<Airline, UUID> {

    Optional<Airline> findByIataCode(String iataCode);

    List<Airline> findByIsActive(Boolean isActive);

    boolean existsByIataCode(String iataCode);
}