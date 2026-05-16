package com.skybooker.airline.service;

import com.skybooker.airline.dto.*;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.exception.AlreadyExistsException;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirlineRepository;
import com.skybooker.airline.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;

    @Override
    public AirlineResponse createAirline(AirlineRequest request) {
        String iata = request.getIataCode().toUpperCase();

        if (airlineRepository.existsByIataCode(iata)) {
            throw new AlreadyExistsException("Airline already exists with IATA code: " + iata);
        }

        Airline airline = Airline.builder()
                .name(request.getName())
                .iataCode(iata)
                .icaoCode(request.getIcaoCode() == null ? null : request.getIcaoCode().toUpperCase())
                .country(request.getCountry())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .isActive(true)
                .build();

        return mapAirline(airlineRepository.save(airline));
    }

    @Override
    public AirlineResponse getAirlineById(UUID airlineId) {
        return mapAirline(getAirlineEntity(airlineId));
    }

    @Override
    public AirlineResponse getAirlineByIata(String iataCode) {
        Airline airline = airlineRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found"));
        return mapAirline(airline);
    }

    @Override
    public List<AirlineResponse> getAllAirlines() {
        return airlineRepository.findAll().stream().map(this::mapAirline).toList();
    }

    @Override
    public List<AirlineResponse> getActiveAirlines() {
        return airlineRepository.findByIsActive(true).stream().map(this::mapAirline).toList();
    }

    @Override
    public AirlineResponse updateAirline(UUID airlineId, AirlineRequest request) {
        Airline airline = getAirlineEntity(airlineId);

        airline.setName(request.getName());
        airline.setIataCode(request.getIataCode().toUpperCase());
        airline.setIcaoCode(request.getIcaoCode() == null ? null : request.getIcaoCode().toUpperCase());
        airline.setCountry(request.getCountry());
        airline.setContactEmail(request.getContactEmail());
        airline.setContactPhone(request.getContactPhone());

        return mapAirline(airlineRepository.save(airline));
    }

    @Override
    public AirlineResponse deactivateAirline(UUID airlineId) {
        Airline airline = getAirlineEntity(airlineId);
        airline.setIsActive(false);
        return mapAirline(airlineRepository.save(airline));
    }

    @Override
    public AirportResponse createAirport(AirportRequest request) {
        String iata = request.getIataCode().toUpperCase();

        if (airportRepository.existsByIataCode(iata)) {
            throw new AlreadyExistsException("Airport already exists with IATA code: " + iata);
        }

        Airport airport = Airport.builder()
                .name(request.getName())
                .iataCode(iata)
                .icaoCode(request.getIcaoCode() == null ? null : request.getIcaoCode().toUpperCase())
                .city(request.getCity())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timezone(request.getTimezone())
                .build();

        return mapAirport(airportRepository.save(airport));
    }

    @Override
    public AirportResponse getAirportByIata(String iataCode) {
        Airport airport = airportRepository.findByIataCode(iataCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found"));
        return mapAirport(airport);
    }

    @Override
    public List<AirportResponse> searchAirports(String keyword) {
        return airportRepository
                .findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrIataCodeContainingIgnoreCase(
                        keyword, keyword, keyword
                )
                .stream()
                .map(this::mapAirport)
                .toList();
    }

    @Override
    public List<AirportResponse> getAirportsByCity(String city) {
        return airportRepository.findByCityIgnoreCase(city).stream().map(this::mapAirport).toList();
    }

    @Override
    public List<AirportResponse> getAirportsByCountry(String country) {
        return airportRepository.findByCountryIgnoreCase(country).stream().map(this::mapAirport).toList();
    }

    @Override
    public AirportResponse updateAirport(UUID airportId, AirportRequest request) {
        Airport airport = airportRepository.findById(airportId)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found"));

        airport.setName(request.getName());
        airport.setIataCode(request.getIataCode().toUpperCase());
        airport.setIcaoCode(request.getIcaoCode() == null ? null : request.getIcaoCode().toUpperCase());
        airport.setCity(request.getCity());
        airport.setCountry(request.getCountry());
        airport.setLatitude(request.getLatitude());
        airport.setLongitude(request.getLongitude());
        airport.setTimezone(request.getTimezone());

        return mapAirport(airportRepository.save(airport));
    }

    private Airline getAirlineEntity(UUID airlineId) {
        return airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found"));
    }

    private AirlineResponse mapAirline(Airline airline) {
        return AirlineResponse.builder()
                .airlineId(airline.getAirlineId())
                .name(airline.getName())
                .iataCode(airline.getIataCode())
                .icaoCode(airline.getIcaoCode())
                .country(airline.getCountry())
                .contactEmail(airline.getContactEmail())
                .contactPhone(airline.getContactPhone())
                .isActive(airline.getIsActive())
                .build();
    }

    private AirportResponse mapAirport(Airport airport) {
        return AirportResponse.builder()
                .airportId(airport.getAirportId())
                .name(airport.getName())
                .iataCode(airport.getIataCode())
                .icaoCode(airport.getIcaoCode())
                .city(airport.getCity())
                .country(airport.getCountry())
                .latitude(airport.getLatitude())
                .longitude(airport.getLongitude())
                .timezone(airport.getTimezone())
                .build();
    }
}