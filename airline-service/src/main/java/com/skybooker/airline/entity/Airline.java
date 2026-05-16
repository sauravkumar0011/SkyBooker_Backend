package com.skybooker.airline.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "airlines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID airlineId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String iataCode;

    @Column(unique = true, length = 4)
    private String icaoCode;

    private String logoUrl;

    private String country;

    private String contactEmail;

    private String contactPhone;

    private Boolean isActive;
}