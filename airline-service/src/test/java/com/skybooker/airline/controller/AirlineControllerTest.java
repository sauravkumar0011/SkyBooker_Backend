package com.skybooker.airline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.airline.dto.*;
import com.skybooker.airline.service.AirlineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AirlineControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AirlineService airlineService;

    private UUID airlineId;

    @BeforeEach
    void setUp() {
        AirlineController controller = new AirlineController(airlineService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        airlineId = UUID.randomUUID();
    }

    private AirlineRequest validAirlineRequest() {
        AirlineRequest request = new AirlineRequest();
        request.setName("Indigo");
        request.setIataCode("6E");
        request.setIcaoCode("IGO");
        request.setCountry("India");
        request.setContactEmail("support@indigo.com");
        request.setContactPhone("9876543210");
        return request;
    }

    private AirportRequest validAirportRequest() {
        AirportRequest request = new AirportRequest();
        request.setName("Delhi Airport");
        request.setIataCode("DEL");
        request.setIcaoCode("VIDP");
        request.setCity("Delhi");
        request.setCountry("India");
        request.setLatitude(28.5562);
        request.setLongitude(77.1000);
        request.setTimezone("Asia/Kolkata");
        return request;
    }

    @Test
    void createAirline_success() throws Exception {
        AirlineResponse response = AirlineResponse.builder()
                .airlineId(airlineId)
                .name("Indigo")
                .iataCode("6E")
                .icaoCode("IGO")
                .country("India")
                .contactEmail("support@indigo.com")
                .contactPhone("9876543210")
                .isActive(true)
                .build();

        when(airlineService.createAirline(any(AirlineRequest.class))).thenReturn(response);

        mockMvc.perform(post("/airlines")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAirlineRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.airlineId").value(airlineId.toString()))
                .andExpect(jsonPath("$.iataCode").value("6E"));
    }

    @Test
    void getAllAirlines_success() throws Exception {
        AirlineResponse response = AirlineResponse.builder()
                .airlineId(airlineId)
                .name("Indigo")
                .iataCode("6E")
                .isActive(true)
                .build();

        when(airlineService.getAllAirlines()).thenReturn(List.of(response));

        mockMvc.perform(get("/airlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].iataCode").value("6E"));
    }

    @Test
    void getActiveAirlines_success() throws Exception {
        AirlineResponse response = AirlineResponse.builder()
                .airlineId(airlineId)
                .name("Indigo")
                .iataCode("6E")
                .isActive(true)
                .build();

        when(airlineService.getActiveAirlines()).thenReturn(List.of(response));

        mockMvc.perform(get("/airlines/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void getAirlineById_success() throws Exception {
        AirlineResponse response = AirlineResponse.builder()
                .airlineId(airlineId)
                .name("Indigo")
                .iataCode("6E")
                .isActive(true)
                .build();

        when(airlineService.getAirlineById(airlineId)).thenReturn(response);

        mockMvc.perform(get("/airlines/{airlineId}", airlineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.airlineId").value(airlineId.toString()));
    }

    @Test
    void getAirlineByIata_success() throws Exception {
        AirlineResponse response = AirlineResponse.builder()
                .airlineId(airlineId)
                .name("Indigo")
                .iataCode("6E")
                .isActive(true)
                .build();

        when(airlineService.getAirlineByIata("6E")).thenReturn(response);

        mockMvc.perform(get("/airlines/iata/{iataCode}", "6E"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("6E"));
    }

    @Test
    void updateAirline_success() throws Exception {
        AirlineResponse response = AirlineResponse.builder()
                .airlineId(airlineId)
                .name("Indigo Updated")
                .iataCode("6E")
                .icaoCode("IGO")
                .country("India")
                .contactEmail("support@indigo.com")
                .contactPhone("9876543210")
                .isActive(true)
                .build();

        when(airlineService.updateAirline(any(UUID.class), any(AirlineRequest.class))).thenReturn(response);

        mockMvc.perform(put("/airlines/{airlineId}", airlineId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAirlineRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Indigo Updated"));
    }

    @Test
    void deactivateAirline_success() throws Exception {
        AirlineResponse response = AirlineResponse.builder()
                .airlineId(airlineId)
                .name("Indigo")
                .iataCode("6E")
                .isActive(false)
                .build();

        when(airlineService.deactivateAirline(airlineId)).thenReturn(response);

        mockMvc.perform(put("/airlines/{airlineId}/deactivate", airlineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void createAirport_success() throws Exception {
        UUID airportId = UUID.randomUUID();

        AirportResponse response = AirportResponse.builder()
                .airportId(airportId)
                .name("Delhi Airport")
                .iataCode("DEL")
                .icaoCode("VIDP")
                .city("Delhi")
                .country("India")
                .latitude(28.5562)
                .longitude(77.1000)
                .timezone("Asia/Kolkata")
                .build();

        when(airlineService.createAirport(any(AirportRequest.class))).thenReturn(response);

        mockMvc.perform(post("/airports")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAirportRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.airportId").value(airportId.toString()))
                .andExpect(jsonPath("$.iataCode").value("DEL"));
    }

    @Test
    void getAirportByIata_success() throws Exception {
        AirportResponse response = AirportResponse.builder()
                .airportId(UUID.randomUUID())
                .name("Delhi Airport")
                .iataCode("DEL")
                .city("Delhi")
                .country("India")
                .build();

        when(airlineService.getAirportByIata("DEL")).thenReturn(response);

        mockMvc.perform(get("/airports/iata/{iataCode}", "DEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("DEL"));
    }

    @Test
    void searchAirports_success() throws Exception {
        AirportResponse response = AirportResponse.builder()
                .airportId(UUID.randomUUID())
                .name("Delhi Airport")
                .iataCode("DEL")
                .city("Delhi")
                .country("India")
                .build();

        when(airlineService.searchAirports("del")).thenReturn(List.of(response));

        mockMvc.perform(get("/airports/search")
                        .param("keyword", "del"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].iataCode").value("DEL"));
    }

    @Test
    void getAirportsByCity_success() throws Exception {
        AirportResponse response = AirportResponse.builder()
                .airportId(UUID.randomUUID())
                .name("Delhi Airport")
                .iataCode("DEL")
                .city("Delhi")
                .country("India")
                .build();

        when(airlineService.getAirportsByCity("Delhi")).thenReturn(List.of(response));

        mockMvc.perform(get("/airports/city/{city}", "Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Delhi"));
    }

    @Test
    void getAirportsByCountry_success() throws Exception {
        AirportResponse response = AirportResponse.builder()
                .airportId(UUID.randomUUID())
                .name("Delhi Airport")
                .iataCode("DEL")
                .city("Delhi")
                .country("India")
                .build();

        when(airlineService.getAirportsByCountry("India")).thenReturn(List.of(response));

        mockMvc.perform(get("/airports/country/{country}", "India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].country").value("India"));
    }

    @Test
    void updateAirport_success() throws Exception {
        UUID airportId = UUID.randomUUID();

        AirportResponse response = AirportResponse.builder()
                .airportId(airportId)
                .name("Delhi Airport Updated")
                .iataCode("DEL")
                .icaoCode("VIDP")
                .city("Delhi")
                .country("India")
                .latitude(28.5562)
                .longitude(77.1000)
                .timezone("Asia/Kolkata")
                .build();

        when(airlineService.updateAirport(any(UUID.class), any(AirportRequest.class))).thenReturn(response);

        mockMvc.perform(put("/airports/{airportId}", airportId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAirportRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Delhi Airport Updated"));
    }
}