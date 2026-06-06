package io.github.kwatera_project.kwatera.property_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResponse(String lat, String lon) {}
