package com.skybooker.airline.service;

import com.skybooker.airline.dto.*;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.exception.AlreadyExistsException;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirlineRepository;
import com.skybooker.airline.repository.AirportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirlineServiceImplTest {

    @Mock
    private AirlineRepository airlineRepository;

    @Mock
    private AirportRepository airportRepository;

    @InjectMocks
    private AirlineServiceImpl airlineService;

    private UUID airlineId;
    private Airline airline;
    private AirlineRequest request;

    @BeforeEach
    void setUp() {
        airlineId = UUID.randomUUID();

        request = new AirlineRequest();
        request.setName("Indigo");
        request.setIataCode("6e");
        request.setIcaoCode("igo");
        request.setCountry("India");
        request.setContactEmail("test@indigo.com");

        airline = Airline.builder()
                .airlineId(airlineId)
                .name("Indigo")
                .iataCode("6E")
                .icaoCode("IGO")
                .country("India")
                .contactEmail("test@indigo.com")
                .isActive(true)
                .build();
    }

    @Test
    void createAirline_success() {
        when(airlineRepository.existsByIataCode("6E")).thenReturn(false);
        when(airlineRepository.save(any())).thenReturn(airline);

        AirlineResponse response = airlineService.createAirline(request);

        assertEquals("6E", response.getIataCode());
        assertTrue(response.getIsActive());
        verify(airlineRepository).save(any());
    }

    @Test
    void createAirline_duplicate_throwException() {
        when(airlineRepository.existsByIataCode("6E")).thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> airlineService.createAirline(request));
    }

    @Test
    void getAirlineById_success() {
        when(airlineRepository.findById(airlineId)).thenReturn(Optional.of(airline));

        AirlineResponse response = airlineService.getAirlineById(airlineId);

        assertEquals(airlineId, response.getAirlineId());
    }

    @Test
    void getAirlineById_notFound() {
        when(airlineRepository.findById(airlineId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> airlineService.getAirlineById(airlineId));
    }

    @Test
    void getAirlineByIata_success() {
        when(airlineRepository.findByIataCode("6E")).thenReturn(Optional.of(airline));

        AirlineResponse response = airlineService.getAirlineByIata("6e");

        assertEquals("6E", response.getIataCode());
    }

    @Test
    void getAllAirlines_success() {
        when(airlineRepository.findAll()).thenReturn(List.of(airline));

        List<AirlineResponse> list = airlineService.getAllAirlines();

        assertEquals(1, list.size());
    }

    @Test
    void getActiveAirlines_success() {
        when(airlineRepository.findByIsActive(true)).thenReturn(List.of(airline));

        List<AirlineResponse> list = airlineService.getActiveAirlines();

        assertTrue(list.get(0).getIsActive());
    }

    @Test
    void updateAirline_success() {
        when(airlineRepository.findById(airlineId)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        request.setName("Updated");

        AirlineResponse response = airlineService.updateAirline(airlineId, request);

        assertEquals("Updated", response.getName());
    }

    @Test
    void deactivateAirline_success() {
        when(airlineRepository.findById(airlineId)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AirlineResponse response = airlineService.deactivateAirline(airlineId);

        assertFalse(response.getIsActive());
    }

    // ---------- AIRPORT TESTS ----------

    @Test
    void createAirport_success() {
        AirportRequest req = new AirportRequest();
        req.setName("Delhi Airport");
        req.setIataCode("del");

        Airport airport = Airport.builder()
                .airportId(UUID.randomUUID())
                .name("Delhi Airport")
                .iataCode("DEL")
                .build();

        when(airportRepository.existsByIataCode("DEL")).thenReturn(false);
        when(airportRepository.save(any())).thenReturn(airport);

        AirportResponse response = airlineService.createAirport(req);

        assertEquals("DEL", response.getIataCode());
    }

    @Test
    void createAirport_duplicate() {
        AirportRequest req = new AirportRequest();
        req.setIataCode("del");

        when(airportRepository.existsByIataCode("DEL")).thenReturn(true);

        assertThrows(AlreadyExistsException.class,
                () -> airlineService.createAirport(req));
    }

    @Test
    void getAirportByIata_success() {
        Airport airport = Airport.builder().iataCode("DEL").build();

        when(airportRepository.findByIataCode("DEL")).thenReturn(Optional.of(airport));

        AirportResponse response = airlineService.getAirportByIata("del");

        assertEquals("DEL", response.getIataCode());
    }

    @Test
    void searchAirports_success() {
        Airport airport = Airport.builder().name("Delhi").build();

        when(airportRepository
                .findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrIataCodeContainingIgnoreCase(
                        any(), any(), any()))
                .thenReturn(List.of(airport));

        List<AirportResponse> list = airlineService.searchAirports("del");

        assertEquals(1, list.size());
    }

    @Test
    void getAirportsByCity_success() {
        when(airportRepository.findByCityIgnoreCase("Delhi"))
                .thenReturn(List.of(Airport.builder().build()));

        assertEquals(1, airlineService.getAirportsByCity("Delhi").size());
    }

    @Test
    void updateAirport_success() {
        UUID id = UUID.randomUUID();
        Airport airport = Airport.builder().airportId(id).name("Old").build();

        AirportRequest req = new AirportRequest();
        req.setName("New");
        req.setIataCode("DEL");

        when(airportRepository.findById(id)).thenReturn(Optional.of(airport));
        when(airportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AirportResponse response = airlineService.updateAirport(id, req);

        assertEquals("New", response.getName());
    }
}