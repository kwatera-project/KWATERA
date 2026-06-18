package io.github.kwatera_project.kwatera.auth_service.dto;

public record AdminUserKpiDto(
    long totalUsers, long totalGuests, long totalOwners, long totalProperties) {}
