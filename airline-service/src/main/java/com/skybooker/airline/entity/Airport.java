package com.skybooker.airline.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "airports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID airportId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String iataCode;

    @Column(unique = true, length = 4)
    private String icaoCode;

    private String city;

    private String country;

    private Double latitude;

    private Double longitude;

    private String timezone;
}