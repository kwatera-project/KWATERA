package io.github.kwatera_project.kwatera.property_service.dto;

public record PropertyCreateRequest(
    String title,
    String description,
    String city,
    String country,
    String postalCode,
    String street,
    String streetNumber) {}
